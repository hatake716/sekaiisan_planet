# ビルドとリリース

Android 11 以降（minSdk 30）、compileSdk / targetSdk 36。JDK 17 以降。

```sh
printf 'sdk.dir=/path/to/Android/Sdk\n' > local.properties
./gradlew testDebugUnitTest lintRelease assembleRelease bundleRelease
```

`local.properties`、`keystore.properties`、鍵、ビルド成果物、取得キャッシュは Git の対象外です。

## 署名

本アプリ専用のアップロード鍵をローカルの `signing/sekaiisan-upload.jks` に作成しています。参照情報は `keystore.properties` に保存しています。他アプリの鍵は使用していません。

鍵と `keystore.properties` は開発者自身が安全な場所にバックアップしてください。GitHub には登録しません。別環境で署名する場合は、自分の鍵を次の形式で設定します。

```properties
storeFile=/absolute/path/to/upload.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

- Play Console 用: `app/build/outputs/bundle/release/app-release.aab`
- 直接インストール用: `app/build/outputs/apk/release/app-release.apk`
- 難読化マッピング: `app/build/outputs/mapping/release/mapping.txt`

AAB は端末に直接インストールできません。Play App Signing を使用する場合、アップロード鍵と Play が配布用 APK に使用する鍵は異なります。

## 検証

`tools/validate.py` が実データ・全写真・出典・ライセンス・座標・カテゴリ・重複を確認します。`CatalogTest` は検索と地域・分類の組み合わせを、元アプリから継承した `CameraTest` はカメラ計算を確認します。

`HeritageFlowTest` はエミュレーター上で、起動、絞り込みの全解除・全選択・適用・キャンセル、検索、詳細、ブックマーク、Activity 再生成、地球儀のドラッグ・ピンチ操作を検証します。

```sh
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
```

日常利用中の実機には入力を注入しないでください。まずエミュレーターで確認した APK を、開発者の判断で実機にインストールします。
