package io.github.hatake716.sekaiisanplanet

import io.github.hatake716.sekaiisanplanet.data.Entry
import io.github.hatake716.sekaiisanplanet.data.HeritageType
import io.github.hatake716.sekaiisanplanet.globe.Camera
import io.github.hatake716.sekaiisanplanet.globe.MarkerLayer
import org.junit.Assert.*
import org.junit.Test

class MarkerLayerTest {
    private fun entry(id: Int, lat: Double, lon: Double) = Entry(
        id = id, name = "Heritage $id", lat = lat, lon = lon, country = "", group = HeritageType.CULTURAL,
        regionMask = 1, desc = "", heritageId = "$id", sourceUrl = "", sourceTitle = "", sourceLang = "ja", image = null,
    )

    @Test fun centeredPinRemainsVisibleAndPickableAcrossEntireZoomRange() {
        for ((lat, lon) in listOf(34.8394 to 134.6939, 0.0 to 179.99, 89.99 to -45.0, -89.99 to 60.0)) {
            val camera = Camera().apply {
                setViewport(1080, 2400)
                centerLat = Math.toRadians(lat); centerLon = Math.toRadians(lon)
            }
            val layer = MarkerLayer(listOf(entry(1, lat, lon)), 3f)
            for (alt in listOf(9.0, 2.2, 0.1, 0.021, 0.02, 0.019, 0.005, Camera.MIN_ALT)) {
                camera.altitude = alt; camera.update(); layer.update(camera)
                assertEquals("lat=$lat alt=$alt", 1, layer.snapshot.count)
                assertEquals(listOf(1), layer.snapshot.pick(540f, 1200f, 30f))
                assertEquals(1, layer.markerCount)
                assertTrue(layer.labels.any { it.id == 1 })
            }
        }
    }

    @Test fun farSideAndBeyondHorizonPinsStayHidden() {
        val camera = Camera().apply {
            setViewport(1080, 2400); centerLat = 0.0; centerLon = 0.0
        }
        for (alt in listOf(9.0, 0.02, Camera.MIN_ALT)) {
            camera.altitude = alt; camera.update()
            val outside = Math.toDegrees(camera.horizonAngle()) + 0.1
            val layer = MarkerLayer(listOf(entry(1, 0.0, 0.0), entry(2, 0.0, outside), entry(3, 0.0, 180.0)), 3f)
            layer.update(camera)
            assertArrayEquals(intArrayOf(1), layer.snapshot.ids)
        }
    }
}
