package io.github.hatake716.sekaiisanplanet.data

import androidx.compose.ui.graphics.Color
import java.net.URLEncoder

/** World Heritage categories; shared by globe pins, filters and the catalogue. */
enum class HeritageType(val label: String, val color: Color) {
    CULTURAL("文化遺産", Color(0xFFFFCC80)),
    NATURAL("自然遺産", Color(0xFF80CBC4)),
    MIXED("複合遺産", Color(0xFFCE93D8));
    companion object { fun fromCode(code: Int): HeritageType = entries[code] }
}

private fun encoded(value: String) = URLEncoder.encode(value.replace(' ', '_'), "UTF-8").replace("+", "%20")

data class ImageCredit(val file: String, val author: String, val license: String, val licenseUrl: String) {
    val commonsUrl: String get() = "https://commons.wikimedia.org/wiki/File:" + encoded(file)
    val line: String get() = "$author / $license"
}

/** One inscribed property. A serial or transboundary property has one representative coordinate. */
data class Entry(
    val id: Int,
    val name: String,
    val aliases: List<String> = emptyList(),
    val englishName: String = "",
    val wikiTitle: String = "",
    val enTitle: String = "",
    val lat: Double,
    val lon: Double,
    val country: String,
    val group: HeritageType,
    val regionMask: Int,
    val importance: Int = 2,
    val desc: String,
    val year: Int = 0,
    val heritageId: String,
    val criteria: String = "",
    val sourceUrl: String,
    val sourceTitle: String,
    val sourceLang: String,
    val categorySourceUrl: String = "",
    val yearSourceUrl: String = "",
    val image: ImageCredit?,
    val yomi: String = "",
) {
    val wikipediaUrl: String get() = if (wikiTitle.isNotBlank()) "https://ja.wikipedia.org/wiki/" + encoded(wikiTitle)
        else "https://en.wikipedia.org/wiki/" + encoded(enTitle)
    val wikipediaDesktopUrl: String get() = wikipediaUrl
    val enWikipediaUrl: String? get() = if (enTitle.isBlank()) null else "https://en.wikipedia.org/wiki/" + encoded(enTitle)
    val youtubeSearchUrl: String get() = "https://m.youtube.com/results?search_query=" + URLEncoder.encode("$name 世界遺産", "UTF-8")
    val hasWikipedia: Boolean get() = wikiTitle.isNotBlank() || enTitle.isNotBlank()
    val imageAsset: String? get() = if (image != null) "img/$id.jpg" else null
    val wikidataUrl: String get() = "https://www.wikidata.org/wiki/Q$id"
    val unescoUrl: String get() = "https://whc.unesco.org/en/list/$heritageId/"
    fun inRegion(regionIndex: Int): Boolean = (regionMask shr regionIndex) and 1 == 1
}
