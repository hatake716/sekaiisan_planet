package io.github.hatake716.sekaiisanplanet

import io.github.hatake716.sekaiisanplanet.data.*
import io.github.hatake716.sekaiisanplanet.globe.MarkerFilter
import org.junit.Assert.*
import org.junit.Test

class CatalogTest {
    private fun entry(id: Int, name: String, group: HeritageType, regions: Int, country: String = "日本") = Entry(
        id = id, name = name, englishName = "Heritage $id", wikiTitle = name, lat = 35.0, lon = 135.0,
        country = country, group = group, regionMask = regions, desc = "説明", heritageId = "$id",
        sourceUrl = "https://ja.wikipedia.org/w/index.php?oldid=1", sourceTitle = name, sourceLang = "ja", image = null)
    private val sites = listOf(entry(1, "法隆寺地域の仏教建造物", HeritageType.CULTURAL, 1),
        entry(2, "屋久島", HeritageType.NATURAL, 1), entry(3, "マチュ・ピチュ", HeritageType.MIXED, 4, "ペルー"),
        entry(4, "ヴィクトリアの滝", HeritageType.NATURAL, 24, "ザンビア・ジンバブエ"))
    private val catalog = Catalog(listOf("日本", "アジア", "南米", "アフリカ", "越境地域"), sites)
    @Test fun launchIncludesAllCategoriesAndRegions() {
        assertTrue(sites.all { MarkerFilter().accepts(it) })
        assertTrue(sites.all { MarkerFilter(HeritageType.entries.toSet(), catalog.regions.indices.toSet()).accepts(it) })
    }
    @Test fun selectionsOrWithinDimensionAndAcrossDimensions() {
        val f = MarkerFilter(setOf(HeritageType.NATURAL, HeritageType.MIXED), setOf(0, 2))
        assertEquals(listOf(2, 3), sites.filter(f::accepts).map { it.id })
    }
    @Test fun emptyDimensionIncludesAllOfThatDimension() {
        assertEquals(listOf(2, 4), sites.filter(MarkerFilter(groups = setOf(HeritageType.NATURAL))::accepts).map { it.id })
        assertEquals(listOf(1, 2), sites.filter(MarkerFilter(regions = setOf(0))::accepts).map { it.id })
    }
    @Test fun transboundaryHeritageMatchesEitherRegion() {
        assertTrue(MarkerFilter(regions = setOf(3)).accepts(sites[3]))
        assertTrue(MarkerFilter(regions = setOf(4)).accepts(sites[3]))
        assertFalse(MarkerFilter(regions = setOf(0)).accepts(sites[3]))
    }
    @Test fun searchesNamesAliasesCountriesAndAlternateKana() {
        assertEquals(2, catalog.search("屋久島").first().id)
        assertEquals(3, catalog.search("ペルー").first().id)
        assertEquals(4, catalog.search("ビクトリア").first().id)
        assertEquals(3, catalog.search("マチュピチュ").first().id)
        assertTrue(catalog.search("存在しない遺産").isEmpty())
    }
    @Test fun wikipediaFallsBackToEnglishWithoutBrokenJapaneseLink() {
        val e = sites[0].copy(wikiTitle = "", enTitle = "A & B")
        assertTrue(e.hasWikipedia)
        assertEquals("https://en.wikipedia.org/wiki/A_%26_B", e.wikipediaUrl)
        assertFalse(e.youtubeSearchUrl.contains(" "))
        assertTrue(e.youtubeSearchUrl.contains("%E4%B8%96%E7%95%8C%E9%81%BA%E7%94%A3"))
    }
    @Test fun imageCreditKeepsAttributionAndEncodedSource() {
        val c = ImageCredit("A & B.jpg", "Photographer", "CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/")
        assertEquals("Photographer / CC BY 4.0", c.line)
        assertEquals("https://commons.wikimedia.org/wiki/File:A_%26_B.jpg", c.commonsUrl)
        assertEquals("img/1.jpg", sites[0].copy(image = c).imageAsset)
    }
}
