# 検証記録（2026-09-06）

対象: 地球儀で見る世界遺産 1.0.0 / versionCode 1 / `io.github.hatake716.sekaiisanplanet`

## データ

1,181 件の遺産、1,181 枚の写真を検査。全件に説明・原典の版・写真の作者・ライセンス・原典・画像 SHA-256 があることを確認。名称・登録 ID の重複、座標範囲、分類、地域ビットマスク、登録年、不要な同梱写真を検査。`tools/validate.py` 通過。

`tools/test_data_policy.py` の 5 テスト通過。商用再利用可能ライセンス、NC / ND / 不明・米国内限定 PD の排除、入れ子テンプレートの複合遺産判定、不明な分類を推測しないことを確認。

## ビルド

`testDebugUnitTest lintRelease assembleRelease bundleRelease assembleDebug assembleDebugAndroidTest` 成功。

- Kotlin 単体テスト 13 件（CameraTest 6、CatalogTest 7）成功。
- Android Lint: エラー 0、警告 23、ヒント 2。主に新しい依存バージョン・推奨記法の案内。
- R8 難読化・リソース縮小を有効にした release APK / AAB を生成。
- APK の署名検証成功（APK Signature Scheme v2、RSA 4096）。
- AAB の署名を確認。Google bundletool 1.18.3 の validate 成功。
- 全 ABI の同梱ネイティブライブラリの ELF LOAD セグメントが 16 KB 以上で整列。APK の `zipalign -c -P 16 4` 成功。

## 実行・操作

Android 15 / API 35 エミュレーター（emulator-5554）で 5 件の UI テスト成功。

- 起動時の全件表示、絞り込みを開いたときの全解除、全選択・全解除、分類と地域の適用、キャンセル、再度開いたときの全解除。
- 日本語検索で姫路城の写真付き詳細を表示。ブックマークが Activity 再生成後も保持されること。
- GL 地球儀のドラッグで中心座標、ピンチで高度が変化すること。
- 実際に描画したピンをタップして詳細を表示。Wikipedia・YouTube ボタンが該当遺産の正しい URL を開くこと。
- 写真の全画面表示、一覧の登録年順・名前順。

署名済み release APK も同じエミュレーターにインストールして、コールド起動・地球儀表示・パッケージ・バージョンを確認。スクリーンショットは `docs/screenshots/`。日常端末へのインストール・入力操作は行っていない。

## 成果物

- APK: 177,287,385 bytes
- AAB: 173,390,711 bytes

```text
524f0600754ebb2912b66286e308501b4e5a012a989337f18ab53d610544bf56  app-release.apk
62636b186f0a15515026743804424730661dd4bde31b52c9dab20053821f292b  app-release.aab
```

Play Console へのアップロード・審査・販売開始は未実施。公開時は対象デバイスでの確認と Console に表示される申告・テスト要件に従う。
