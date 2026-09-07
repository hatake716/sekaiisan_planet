package io.github.hatake716.sekaiisanplanet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hatake716.sekaiisanplanet.BuildConfig
import io.github.hatake716.sekaiisanplanet.data.Catalog

@Composable
fun AboutDialog(catalog: Catalog, onDismiss: () -> Unit, onCredits: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
        dismissButton = { TextButton(onClick = onCredits) { Text("写真の出典一覧") } },
        title = { Text("地球儀で見る世界遺産") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("バージョン ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text("世界の ${catalog.entries.size} 件の世界遺産を地球儀で巡るアプリです。写真・説明・地球画像はオフラインでも見られます。")
            Spacer(Modifier.height(12.dp))
            Text("操作", fontWeight = FontWeight.Bold)
            Text("ドラッグで北極・南極を越えて回転、ピンチで拡大縮小、ダブルタップで拡大。ピンをタップすると写真と詳細を表示します。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text("絞り込み", fontWeight = FontWeight.Bold)
            Text("起動時は全件を表示します。絞り込みを開くたびにチェックは全解除から始まります。分類や地域を選んで「この条件で表示」を押してください。「全選択」の下に「全解除」があります。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text("出典と利用条件", fontWeight = FontWeight.Bold)
            Text("写真: Wikimedia Commons の CC0・パブリックドメイン・CC BY・CC BY-SA。写真ごとに作者・原典・利用条件を表示します。\n説明: Wikipedia の記事からの抜粋（CC BY-SA 4.0）。各説明に原文の版へのリンクを記載しています。\n登録情報・座標: Wikidata（CC0）。データは取得時点のスナップショットです。登録物件の範囲が広い場合は代表地点にピンを置いています。全登録物件の網羅や最新の登録状況を保証するものではありません。\n地球画像: NASA Blue Marble（パブリックドメイン）。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text("プライバシーポリシー", fontWeight = FontWeight.Bold)
            Text("広告・解析・アカウント登録はありません。ブックマークと閲覧履歴は端末内に保存します。外部ページを開くと各提供元に接続し、IPアドレスやCookie等はその提供元の方針に従って扱われます。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text("UNESCO・Google・Wikipedia の公式アプリではありません。\nアプリ本体: MIT License\n元の地球儀実装: hatake716/animal_planet\nソースコード: github.com/hatake716/sekaiisan_planet", style = MaterialTheme.typography.bodySmall)
        } })
}
