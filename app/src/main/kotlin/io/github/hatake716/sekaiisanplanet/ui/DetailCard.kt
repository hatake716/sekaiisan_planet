package io.github.hatake716.sekaiisanplanet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hatake716.sekaiisanplanet.data.Catalog
import io.github.hatake716.sekaiisanplanet.data.Entry

/** 画面下部に出る遺産のカード。地球儀は背後で操作できる。 */
@Composable
fun DetailCard(
    entry: Entry,
    catalog: Catalog,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    onWikipedia: () -> Unit,
    onYoutube: () -> Unit,
    onZoom: () -> Unit,
    onShare: () -> Unit,
    onPhoto: () -> Unit,
    onCommons: () -> Unit,
    onOpenSource: (String, String) -> Unit,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 8.dp),
        shape = RoundedCornerShape(22.dp),
        // 半透明にすると背後の明るい地図が透けて文字が読みにくくなるため、少し濃い色で不透明にする
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        // 横画面など高さが足りない画面でもボタン行が押し出されないよう、カード全体の高さを画面の 88% までに抑え、
        // 展開時の本文(スクロール)だけを伸縮させる。
        val screenH = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp().value }
        Column(
            Modifier
                .heightIn(max = (screenH * 0.88f).dp)
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 14.dp)
                .animateContentSize(),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                if (!expanded && entry.image != null) {
                    HeritageImage(entry, maxSide = 240, modifier = Modifier.size(84.dp).clickable(onClick = onPhoto), cornerRadius = 12.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f).clickable(onClick = onToggleExpand).padding(start = if (expanded) 4.dp else 0.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        YearBadge(entry.year)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(9.dp).background(entry.group.color, CircleShape))
                        Spacer(Modifier.width(5.dp))
                        Text(entry.group.label, style = MaterialTheme.typography.labelMedium, color = entry.group.color)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (expanded) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.englishName.isNotBlank()) {
                        Text(entry.englishName, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (!expanded && entry.country.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(entry.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (bookmarked) "ブックマーク解除" else "ブックマーク",
                        tint = if (bookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess, contentDescription = if (expanded) "折りたたむ" else "詳細")
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "閉じる") }
            }
            if (expanded) {
                ExpandedBody(entry, catalog, compact = screenH < 560, onPhoto, onCommons, onOpenSource)
            } else if (entry.desc.isNotBlank()) {
                Text(
                    entry.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 6.dp, end = 10.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.padding(top = 10.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onWikipedia,
                    enabled = entry.hasWikipedia,
                    modifier = Modifier.weight(1.35f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (entry.hasWikipedia) "Wikipedia" else "記事なし", maxLines = 1, softWrap = false)
                }
                FilledTonalButton(
                    onClick = onYoutube,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("動画", maxLines = 1, softWrap = false)
                }
                IconButton(onClick = onZoom) { Icon(Icons.Default.ZoomIn, contentDescription = "所在地を拡大") }
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "共有") }
            }
        }
    }
}

@Composable
private fun ColumnScope.ExpandedBody(entry: Entry, catalog: Catalog, compact: Boolean, onPhoto: () -> Unit, onCommons: () -> Unit, onOpenSource: (String, String) -> Unit) {
    Column(Modifier.weight(1f, fill = false).heightIn(max = if (compact) 240.dp else 480.dp)
        .padding(top = 8.dp, end = 8.dp).verticalScroll(rememberScrollState())) {
        entry.image?.let { credit ->
            HeritageImage(entry, maxSide = 800, modifier = Modifier.fillMaxWidth().height(if (compact) 130.dp else 205.dp).clickable(onClick = onPhoto), cornerRadius = 14.dp)
            Text("写真: ${credit.line}\n縮小・JPEG変換 / Wikimedia Commons", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp).clickable(onClick = onCommons))
            androidx.compose.material3.TextButton(onClick = { onOpenSource(credit.license, credit.licenseUrl) }) { Text("写真の利用条件") }
        }
        Text(entry.country, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        val regions = catalog.regions.withIndex().filter { entry.inRegion(it.index) }.joinToString("・") { it.value }
        Text("$regions / ${entry.group.label}", style = MaterialTheme.typography.bodySmall, color = entry.group.color)
        Text(if (entry.year > 0) "${entry.year}年登録 / 世界遺産 ID ${entry.heritageId}" else "世界遺産 ID ${entry.heritageId} / 登録年データなし",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        if (entry.criteria.isNotBlank()) Text("登録基準: ${entry.criteria}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${if (entry.lat >= 0) "北緯" else "南緯"} ${"%.4f".format(kotlin.math.abs(entry.lat))}° / ${if (entry.lon >= 0) "東経" else "西経"} ${"%.4f".format(kotlin.math.abs(entry.lon))}°（代表地点）",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (entry.aliases.isNotEmpty()) Text("別名: ${entry.aliases.joinToString(" / ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("この世界遺産について", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        if (entry.sourceLang == "en") Text("英語版 Wikipedia の説明", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(entry.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("説明: Wikipedia「${entry.sourceTitle}」の執筆者 / CC BY-SA 4.0。記事から抜粋し、脚注・書式を整理しています。",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        androidx.compose.material3.TextButton(onClick = { onOpenSource("説明の出典", entry.sourceUrl) }) { Text("説明の原文・編集履歴") }
        androidx.compose.material3.TextButton(onClick = { onOpenSource("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/deed.ja") }) { Text("説明の利用条件（CC BY-SA 4.0）") }
        androidx.compose.material3.TextButton(onClick = { onOpenSource("登録情報・座標の出典", entry.wikidataUrl) }) { Text("登録情報・座標: Wikidata（CC0）") }
        androidx.compose.material3.TextButton(onClick = { onOpenSource("登録年の出典", entry.yearSourceUrl) }) { Text("登録年の出典を確認") }
        androidx.compose.material3.TextButton(onClick = { onOpenSource("分類の出典", entry.categorySourceUrl) }) { Text("分類の出典を確認") }
        androidx.compose.material3.TextButton(onClick = { onOpenSource(entry.name, entry.unescoUrl) }) { Text("UNESCO の登録ページ") }
    }
}
