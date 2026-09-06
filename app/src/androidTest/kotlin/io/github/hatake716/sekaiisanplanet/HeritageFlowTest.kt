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
        ui.onNodeWithTag("globe").performTouchInput { pinch(center.copy(x=center.x-40f),center.copy(x=center.x+40f),center.copy(x=center.x-160f),center.copy(x=center.x+160f),700) }
        ui.waitUntil(12_000) { vm.savedAlt != alt }
        screenshot("04_zoom")
    }
    private fun findGlobe(view: View): GlobeView? {
        if (view is GlobeView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findGlobe(view.getChildAt(i))?.let { return it }
        return null
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
        ui.runOnIdle { vm.photoOfId=-1;vm.clearSelection();vm.showList=true }
        ui.onNodeWithText("登録年順").performClick()
        screenshot("07_catalogue")
        ui.onNodeWithText("名前順").performClick()
    }

}
