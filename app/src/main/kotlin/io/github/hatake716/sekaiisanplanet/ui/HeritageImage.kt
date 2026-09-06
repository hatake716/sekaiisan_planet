package io.github.hatake716.sekaiisanplanet.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hatake716.sekaiisanplanet.data.Entry
import io.github.hatake716.sekaiisanplanet.data.ImageStore

/**
 * 同梱写真(assets/img/{id}.jpg)を表示する。写真がない種は分類色の薄い背景にアイコンを出す。
 * @param maxSide デコード時の長辺上限(px)。一覧は小さく、詳細は大きく。
 *
 * 状態は remember(path, maxSide) でキーごとに作り直す。produceState はキーが変わっても内部の値を
 * 引き継ぐため、別の種に切り替えた瞬間に前の遺産の写真が残る(名前と写真が食い違う)。
 */
@Composable
fun HeritageImage(
    entry: Entry,
    maxSide: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadius: Dp = 0.dp,
    tintBackground: Boolean = true,
) {
    val context = LocalContext.current
    val path = entry.imageAsset
    val store = ImageStore.get(context)
    val bitmapState = remember(path, maxSide) { mutableStateOf<Bitmap?>(if (path == null) null else store.peek(path, maxSide)) }
    LaunchedEffect(path, maxSide) {
        if (path != null && bitmapState.value == null) bitmapState.value = store.load(path, maxSide)
    }
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (tintBackground) entry.group.color.copy(alpha = 0.16f) else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmapState.value
        if (bmp != null) {
            Image(bmp.asImageBitmap(), contentDescription = entry.name, modifier = Modifier.fillMaxSize(), contentScale = contentScale)
        } else if (path == null) {
            Icon(Icons.Default.AccountBalance, contentDescription = "写真なし", tint = entry.group.color.copy(alpha = 0.75f), modifier = Modifier.fillMaxSize(0.45f))
        }
    }
}

/** Year of first inscription, when recorded by Wikidata. */
@Composable
fun YearBadge(year: Int, modifier: Modifier = Modifier) {
    if (year > 0) Text(
        "${year}年", color = Color(0xFFFFDDA8),
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.background(Color(0xFF4B3B27), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
