# 第三者素材・データの出典と利用条件

アプリのコードの MIT License と、写真・説明・データの利用条件は別です。配布・商用販売を含む再利用を許諾している情報源だけを採用しています。

## 元のプログラム

地球儀、カメラ・タイル・マーカー描画、Compose UI、写真表示、端末内保存、WebView は [hatake716/animal_planet](https://github.com/hatake716/animal_planet) のコミット `d220f5c3e1969872ed5e1c70be4d1a216ca9bdbd` を基に改修しました。元の MIT ライセンスと著作権表示を `LICENSE` に保持しています。

## 世界遺産の説明

Wikipedia 日本語版・英語版の記事冒頭と本文の一部を抜粋しています。本文は **CC BY-SA 4.0** で提供します。脚注・マークアップを整理し、長い本文は文の区切りで抜粋しています。各説明の執筆者はリンク先の記事の執筆者です。

- 個々の記事名・言語・取得した版への URL は、詳細画面および `data/provenance.json` に記載。
- 原文の版のページから編集履歴を確認できます。
- 翻案・抜粋した説明も CC BY-SA 4.0 で再利用できます。アプリのコード全体のライセンスとは別に扱ってください。
- [CC BY-SA 4.0（日本語）](https://creativecommons.org/licenses/by-sa/4.0/deed.ja)
- [Wikimedia 利用規約](https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use)

## 登録情報・座標

Wikidata の名称、別名、国、世界遺産 ID、登録年、登録基準、代表座標を使用しています（**CC0 1.0**）。分類は Wikipedia の世界遺産概要表の記載を優先し、不足する場合は Wikidata の登録基準から判定しています。分類の出典も個別にリンクしています。地域は国の大陸情報と代表地点に基づく本アプリ独自の区分です。

- [Wikidata:Licensing](https://www.wikidata.org/wiki/Wikidata:Licensing)
- [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/deed.ja)

## 写真

Wikimedia Commons のファイルごとのライセンス・作者・利用条件を取得して検査しています。CC0、パブリックドメイン、CC BY、CC BY-SA の画像のみを使用します。NC（非商用）・ND（改変禁止）・フェアユース・GFDL 単独・不明なライセンス・追加制限のある画像は除外します。帰属が必要なのに作者が確認できない画像も採用しません。

長辺 800 ピクセル以下に縮小し、JPEG に変換しています。表示枠によるトリミングがありますが、写真を開くと全体を表示します。写真の作者、タイトル、元のファイルページ、ライセンス名と URL はアプリ内、`docs/CREDITS.md`、`data/provenance.json` に記載しています。CC BY-SA の写真の加工物は、その写真と同じライセンスで提供します。

[Commons の再利用ガイド](https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia)

## 地球画像

NASA Blue Marble: Next Generation（world.topo.bathy.200412）。NASA Earth Observatory / NASA Goddard Space Flight Center、Reto Stöckli ほか。パブリックドメイン。元アプリの同梱タイルを使用しています。

- [Blue Marble: Next Generation](https://earthobservatory.nasa.gov/features/BlueMarble)
- [NASA 画像の利用方針](https://www.nasa.gov/nasa-brand-center/images-and-media/)

## アイコンと外部リンク

本アプリの建造物アイコンとストア用の SVG グラフィックは本プロジェクトで制作し、MIT License で提供しています。
Wikipedia・YouTube・UNESCO のページはリンク先を閲覧する仕組みです。YouTube 動画・サムネイル、UNESCO の写真・説明文はアプリに複製していません。公式ロゴも使用していません。
