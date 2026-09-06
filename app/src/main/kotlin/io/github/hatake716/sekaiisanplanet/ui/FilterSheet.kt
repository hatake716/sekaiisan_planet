package io.github.hatake716.sekaiisanplanet.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hatake716.sekaiisanplanet.data.Catalog
import io.github.hatake716.sekaiisanplanet.data.HeritageType
import kotlinx.coroutines.launch

/** Each opening starts with an empty draft; cancel preserves the applied globe filter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(vm: MainViewModel, catalog: Catalog, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val f = vm.filterDraft
    val nothingChecked = f.groups.isEmpty() && f.regions.isEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("絞り込み", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (nothingChecked) "見たい項目を選んでください" else "${catalog.entries.count { f.accepts(it) }} 件が該当",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = vm::selectAllFilter) { Text("全選択") }
                    TextButton(onClick = vm::deselectAllFilter) { Text("全解除") }
                }
            }
            Text("地域と分類から、見たい項目にチェックを入れてください。片方だけ選ぶと、もう片方はすべて対象になります。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("世界遺産の分類", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (g in io.github.hatake716.sekaiisanplanet.data.HeritageType.entries) {
                    FilterChip(modifier = Modifier.testTag("filter-group-${g.ordinal}"), selected = g in f.groups, onClick = { vm.toggleGroupFilter(g) }, label = { Text(g.label) },
                        leadingIcon = { androidx.compose.material3.Checkbox(checked = g in f.groups, onCheckedChange = null, modifier = Modifier.size(18.dp)) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("地域", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((i, region) in catalog.regions.withIndex()) {
                    FilterChip(selected = i in f.regions, onClick = { vm.toggleRegionFilter(i) }, label = { Text(region) },
                        leadingIcon = { androidx.compose.material3.Checkbox(checked = i in f.regions, onCheckedChange = null, modifier = Modifier.size(18.dp)) })
                }
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Button(onClick = vm::applyFilter, enabled = !nothingChecked, modifier = Modifier.fillMaxWidth()) { Text("この条件で表示") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("キャンセル") }
        }
    }
}
