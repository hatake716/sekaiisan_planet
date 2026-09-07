package io.github.hatake716.sekaiisanplanet

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import io.github.hatake716.sekaiisanplanet.ui.MainViewModel
import io.github.hatake716.sekaiisanplanet.data.HeritageType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import io.github.hatake716.sekaiisanplanet.globe.GlobeView
import io.github.hatake716.sekaiisanplanet.globe.Camera
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/** Emulator end-to-end coverage: actual packaged catalogue, Compose controls, GL gestures and persistence. */
class HeritageFlowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private fun model(): MainViewModel = ViewModelProvider(ui.activity)[MainViewModel::class.java]
    private fun ready(): MainViewModel {
        ui.waitUntil(30_000) { model().catalog != null }
        ui.waitForIdle()
        return model()
    }
    private fun screenshot(name: String) {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.waitForIdle()
        val dir=File(ui.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        device.takeScreenshot(File(dir,"$name.png"))
    }
    @Test fun startupAndFilterDraftApplyCancelReopen() {
        val vm=ready();val c=vm.catalog!!
        assertTrue(c.entries.size > 700)
        assertTrue(c.entries.all(vm.filter::accepts))
        screenshot("01_globe")
        ui.onNodeWithContentDescription("絞り込み").performClick()
        ui.onNodeWithText("この条件で表示").assertIsNotEnabled()
        ui.onNodeWithTag("filter-group-0").performClick()
        ui.onNodeWithText("日本", useUnmergedTree=true).performClick()
        screenshot("02_filters")
        ui.onNodeWithText("この条件で表示").performClick()
        ui.runOnIdle {
            val shown=c.entries.filter(vm.filter::accepts)
            assertTrue(shown.isNotEmpty());assertTrue(shown.all { it.group == HeritageType.CULTURAL && it.inRegion(0) })
        }
        val before=vm.filter
        ui.onNodeWithContentDescription("絞り込み").performClick()
        ui.onNodeWithText("この条件で表示").assertIsNotEnabled()
        ui.onNodeWithText("キャンセル").performClick()
        ui.runOnIdle { assertEquals(before,vm.filter) }
        ui.onNodeWithContentDescription("絞り込み").performClick()
        ui.onNodeWithText("全選択").performClick()
        ui.onNodeWithText("この条件で表示").assertIsEnabled()
        ui.onNodeWithText("全解除").performClick()
        ui.onNodeWithText("この条件で表示").assertIsNotEnabled()
        ui.onNodeWithText("全選択").performClick()
        ui.onNodeWithText("この条件で表示").performClick()
        ui.runOnIdle { assertTrue(c.entries.all(vm.filter::accepts)) }
    }
    @Test fun searchOpensPhotographedHeritageAndBookmarkSurvivesRecreation() {
        val vm=ready()
        ui.onNode(hasSetTextAction()).performTextInput("姫路")
        ui.waitUntil(5_000) { vm.searchResults.isNotEmpty() }
        val e=vm.searchResults.first()
        ui.onNodeWithText(e.name, useUnmergedTree=true).performClick()
        ui.waitUntil(5_000) { vm.selectedId == e.id }
        ui.onNodeWithText("Wikipedia").assertIsDisplayed()
        ui.onNodeWithText("動画").assertIsDisplayed()
        screenshot("03_detail")
        if (!vm.isBookmarked(e.id)) ui.onNodeWithContentDescription("ブックマーク").performClick()
        ui.waitUntil(5_000) { vm.isBookmarked(e.id) }
        ui.activityRule.scenario.recreate()
        ui.waitUntil(10_000) { model().isBookmarked(e.id) }
        assertEquals(e.id,model().selectedId)
    }
    @Test fun globeAcceptsDragAndPinchAndPackagedDataHasRealCoordinates() {
        val vm=ready();val c=vm.catalog!!
        assertEquals(3,c.entries.map { it.group }.toSet().size)
        assertTrue(c.entries.all { it.lat in -90.0..90.0 && it.lon in -180.0..180.0 && it.desc.length >= 100 })
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val before=vm.savedLon
        ui.onNodeWithTag("globe").performTouchInput { swipe(center, center.copy(x=center.x+180f),500) }
        ui.waitUntil(12_000) { vm.savedLon != before }
        val alt=vm.savedAlt
        ui.onNodeWithTag("globe").performTouchInput {
            pinch(start0=center.copy(x=center.x-40f), end0=center.copy(x=center.x-160f),
                start1=center.copy(x=center.x+40f), end1=center.copy(x=center.x+160f), durationMillis=700)
        }
        ui.waitUntil(12_000) { vm.savedAlt < alt }
        screenshot("04_zoom")
    }
    private fun findGlobe(view: View): GlobeView? {
        if (view is GlobeView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findGlobe(view.getChildAt(i))?.let { return it }
        return null
    }

    private fun <T> onGl(globe: GlobeView, block: () -> T): T {
        val result = AtomicReference<T>()
        val done = CountDownLatch(1)
        globe.queueEvent { result.set(block()); done.countDown() }
        assertTrue("GL thread responded", done.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    @Test fun maximumZoomKeepsVisiblePinAndTapOpensItsDetails() {
        val vm = ready()
        val entry = vm.catalog!!.entries.first { it.name == "姫路城" }
        val globe = findGlobe(ui.activity.window.decorView)!!
        ui.runOnIdle { vm.clearSelection(); globe.setCamera(entry.lat, entry.lon, 0.03) }
        ui.waitUntil(10_000) { abs(globe.renderer.markers.snapshot.altitude - 0.03) < 1e-6 }
        repeat(6) {
            val expected = (globe.renderer.markers.snapshot.altitude / 2).coerceAtLeast(Camera.MIN_ALT)
            ui.onNodeWithContentDescription("拡大").performClick()
            ui.waitUntil(5_000) { abs(globe.renderer.markers.snapshot.altitude - expected) < 1e-8 }
            assertTrue("pin remains at altitude $expected", entry.id in globe.renderer.markers.snapshot.ids)
        }
        assertEquals(Camera.MIN_ALT, globe.renderer.markers.snapshot.altitude, 1e-8)
        screenshot("08_maximum_zoom")
        val snapshot = globe.renderer.markers.snapshot
        val index = snapshot.ids.indexOf(entry.id)
        ui.onNodeWithTag("globe").performTouchInput { click(Offset(snapshot.xs[index], snapshot.ys[index])) }
        ui.waitUntil(5_000) { vm.selectedId == entry.id }
        ui.onNodeWithText("Wikipedia").assertIsDisplayed()
        screenshot("09_maximum_zoom_detail")
    }

    @Test fun dragCrossesBothPolesWithoutStickingAndRetainsOrientationOnZoomAndRecreation() {
        val vm = ready()
        val globe = findGlobe(ui.activity.window.decorView)!!
        for (sign in listOf(1, -1)) {
            ui.runOnIdle { vm.clearSelection(); globe.setCamera(sign * 85.0, 40.0, 1.5) }
            ui.waitUntil(10_000) { abs(onGl(globe) { Math.toDegrees(globe.renderer.camera.centerLat) } - sign * 85.0) < 0.001 }
            val point = onGl(globe) { globe.renderer.camera.surfacePoint(globe.width / 2f, globe.height / 2f)!! }
            ui.onNodeWithTag("globe").performTouchInput {
                down(center)
                for (step in 1..16) moveTo(center.copy(y = center.y + sign * height * 0.20f * step / 16), delayMillis = 30)
                cancel()
            }
            val after = onGl(globe) {
                globe.renderer.camera.update()
                Camera().also { it.setViewport(globe.width, globe.height); it.copyFrom(globe.renderer.camera); it.update() }
            }
            assertTrue("crossed ${sign * 90} degrees", abs(Math.toDegrees(Camera.wrapLon(after.centerLon - Math.toRadians(40.0)))) > 170)
            assertTrue("kept moving past pole", abs(Math.toDegrees(after.centerLat)) < 85)
            val projected = FloatArray(3)
            assertTrue(after.project(point[0], point[1], point[2], projected))
            assertEquals(globe.width / 2f, projected[0], 2f)
            assertEquals(globe.height * (0.5f + sign * 0.20f), projected[1], 2f)
            screenshot(if (sign > 0) "10_north_pole_crossing" else "11_south_pole_crossing")
        }
        val orientation = onGl(globe) { globe.renderer.camera.rotation }
        ui.onNodeWithContentDescription("拡大").performClick()
        ui.waitUntil(5_000) { globe.renderer.markers.snapshot.altitude < 0.751 }
        val afterZoom = onGl(globe) { globe.renderer.camera.rotation }
        assertTrue((afterZoom * orientation.inverse()).vector().sumOf { it * it } < 1e-10)
        ui.waitUntil(10_000) { vm.savedRotation != null && abs(vm.savedAlt - 0.75) < 1e-6 }
        ui.activityRule.scenario.recreate()
        ready()
        val restored = findGlobe(ui.activity.window.decorView)!!
        ui.waitUntil(10_000) { restored.renderer.markers.snapshot.altitude > 0 }
        val restoredOrientation = onGl(restored) { restored.renderer.camera.rotation }
        assertTrue((restoredOrientation * orientation.inverse()).vector().sumOf { it * it } < 1e-10)
        ui.onNodeWithContentDescription("地球全体を表示").performClick()
        ui.waitUntil(5_000) { restored.renderer.markers.snapshot.altitude > 1.5 }
        assertTrue((onGl(restored) { restored.renderer.camera.rotation } * orientation.inverse()).vector().sumOf { it * it } < 1e-10)
    }

    @Test fun polarFlingPinchAndDoubleTapRemainUsable() {
        val vm = ready()
        val globe = findGlobe(ui.activity.window.decorView)!!
        ui.runOnIdle { vm.clearSelection(); globe.setCamera(87.0, 170.0, 1.5) }
        ui.waitUntil(10_000) { abs(onGl(globe) { Math.toDegrees(globe.renderer.camera.centerLat) } - 87.0) < 0.001 }
        ui.onNodeWithTag("globe").performTouchInput { swipe(center, center.copy(y = center.y + height * 0.08f), 200) }
        val released = onGl(globe) { globe.renderer.camera.rotation }
        ui.waitUntil(3_000) { (onGl(globe) { globe.renderer.camera.rotation } * released.inverse()).vector().sumOf { it * it } > 1e-6 }
        onGl(globe) { globe.renderer.stopAnimations() }
        val before = onGl(globe) { globe.renderer.camera.rotation }
        ui.onNodeWithTag("globe").performTouchInput { doubleClick(center) }
        ui.waitUntil(5_000) { abs(globe.renderer.markers.snapshot.altitude - 0.6) < 1e-6 }
        assertTrue((onGl(globe) { globe.renderer.camera.rotation } * before.inverse()).vector().sumOf { it * it } < 1e-8)
        val focalX = globe.width * 0.58f
        val focalY = globe.height * 0.56f
        val point = onGl(globe) { globe.renderer.camera.surfacePoint(focalX, focalY)!! }
        ui.onNodeWithTag("globe").performTouchInput {
            // 独立したピンチとして、前のダブルタップからタッチ時刻を空ける。
            advanceEventTime(600)
            pinch(start0=Offset(focalX-50, focalY), end0=Offset(focalX-140, focalY),
                start1=Offset(focalX+50, focalY), end1=Offset(focalX+140, focalY), durationMillis=700)
        }
        ui.waitUntil(5_000) { globe.renderer.markers.snapshot.altitude < 0.3 }
        val projected = onGl(globe) {
            globe.renderer.camera.update()
            FloatArray(3).also { globe.renderer.camera.project(point[0], point[1], point[2], it) }
        }
        assertEquals(focalX, projected[0], 3f); assertEquals(focalY, projected[1], 3f)
        screenshot("12_polar_pinch")
    }
    @Test fun tapRenderedPinOpensPhotoAndSourceLinks() {
        val vm=ready()
        val globe=findGlobe(ui.activity.window.decorView)!!
        ui.runOnIdle { vm.clearSelection(); globe.setCamera(30.0, 110.0, 0.0) }
        ui.waitUntil(10_000) { globe.renderer.markers.snapshot.count>0 }
        val snap=globe.renderer.markers.snapshot
        val index=(0 until snap.count).first { snap.reps[it] && snap.xs[it] in globe.width*.18f..globe.width*.8f && snap.ys[it] in globe.height*.28f..globe.height*.68f }
        ui.onNodeWithTag("globe").performTouchInput { click(Offset(snap.xs[index],snap.ys[index])) }
        ui.waitUntil(8_000) { vm.selectedEntry!=null || vm.pickCandidates.isNotEmpty() }
        if (vm.pickCandidates.isNotEmpty()) {
            val first=vm.pickCandidates.first()
            ui.onNodeWithText(first.name,useUnmergedTree=true).performClick()
        }
        ui.waitUntil(5_000) { vm.selectedEntry!=null }
        val selected=vm.selectedEntry!!
        assertNotNull(selected.image)
        ui.onNodeWithText("Wikipedia").performClick()
        ui.runOnIdle { assertEquals(selected.wikipediaUrl,vm.webUrl) }
        ui.onNodeWithContentDescription("ブラウザで開く").assertIsDisplayed()
        screenshot("05_wikipedia")
        ui.runOnIdle { vm.closeWeb() }
        ui.onNodeWithText("動画").performClick()
        ui.runOnIdle { assertEquals(selected.youtubeSearchUrl,vm.webUrl) }
        ui.onNodeWithContentDescription("ブラウザで開く").assertIsDisplayed()
        ui.runOnIdle { vm.closeWeb() }
    }
    @Test fun listPhotoAndOfflineDetails() {
        val vm=ready()
        ui.onNode(hasSetTextAction()).performTextInput("姫路")
        ui.waitUntil(5_000) { vm.searchResults.isNotEmpty() }
        val entry=vm.searchResults.first()
        assertNotNull(entry.image)
        ui.onNodeWithText(entry.name,useUnmergedTree=true).performClick()
        ui.waitUntil(5_000) { vm.selectedId==entry.id }
        ui.onNodeWithContentDescription(entry.name).performClick()
        ui.waitUntil(5_000) { vm.photoOfId==entry.id }
        screenshot("06_photo")
        ui.runOnIdle { vm.photoOfId=-1;vm.clearSelection() }
        ui.waitForIdle()
        ui.onNodeWithContentDescription("世界遺産一覧").performClick()
        ui.waitUntil(5_000) { ui.onAllNodesWithText("登録年順").fetchSemanticsNodes().size == 1 }
        ui.onNodeWithText("登録年順").performClick()
        screenshot("07_catalogue")
        ui.onNodeWithText("名前順").performClick()
    }

}
