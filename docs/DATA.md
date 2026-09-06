# データの生成と監査

## 入力

- Wikidata Query Service: P757（世界遺産 ID）。構成資産用の枝番を除き、P1435 に現在の世界遺産登録がある項目を候補とする。
- Wikidata API: labels / aliases / P17 / P30 / P625 / P1435（登録年・基準）/ P18 / sitelinks。
- Wikipedia API: 記事の版 ID、導入部・本文のプレーンテキスト、世界遺産概要表、記事の代表画像。
- Wikimedia Commons API: ファイルごとの作者、原典、ライセンス、利用制限、サムネイル。

本文を持たない登録物件は、Wikidata の構成資産・所在地の記事も確認し、Wikipedia の概要表に同一の世界遺産 ID が明記されている場合にだけ結び付ける。名称の類似だけで無関係な観光地に結び付けない。採用できない候補は `data/exclusions.json` に理由を記録する。暫定リストや登録解除済みは候補選定で除外する。

## 実行

Python 3 と Pillow を使用する。キャッシュは `data/cache/` に保存する（Git 対象外）。正常取得済みのデータは再利用し、途中停止後も再開できる。更新日にキャッシュを別ディレクトリへ退避すれば新しいスナップショットを取得できる。

```sh
python3 -m pip install -r tools/requirements.txt
python3 tools/fetch_catalog.py
python3 tools/fetch_content.py
python3 tools/resolve_articles.py
python3 tools/expand_descriptions.py
python3 tools/fetch_photos.py --fallback
python3 tools/assemble_catalog.py --final
python3 tools/validate.py
```

データ取得は接続先に負荷をかけない並列度で行い、User-Agent、再試行、チェックポイントを使う。標準の証明書検証を有効にしたまま使用する。環境固有の CA が必要な場合は `SSL_CERT_FILE` で信頼済み証明書バンドルを指定する。

`--final` は、出典付き写真のない項目を除外し、採用しない写真をキャッシュへ退避して、アプリの写真すべてに帰属情報がある状態を作る。写真がなくなったりライセンス条件が変わった場合に、別の未確認素材で穴埋めしない。

## 出力と限界

- `app/src/main/assets/heritage.json`: アプリが読む実データ。
- `app/src/main/assets/img/`: 採用写真。
- `data/provenance.json`: 項目ごとの説明の版、Wikidata の版、写真の原典・作者・ライセンス・加工・SHA-256。
- `data/catalog-summary.json`: 収録数、写真数、分類と言語の内訳。
- `data/exclusions.json`: 候補のうち掲載できなかった項目と理由。
- `docs/CREDITS.md`: 全写真の出典と説明の出典一覧。

このアプリは取得時点の資料に基づく図鑑であり、全世界遺産の完全な登録簿ではない。広域・越境・連続資産は代表座標 1 点で表示する。国名・表記・登録年は取得元に依存し、公式な境界表示・ナビゲーションには使用しない。分類と登録基準の出典が異なる場合は、それぞれのリンクを確認できる。

## ライセンス

Wikidata の構造化情報は CC0。Wikipedia 由来の抜粋・分類情報は CC BY-SA 4.0。写真は各ファイルの CC0 / PD / CC BY / CC BY-SA。`NOTICE.md` の条件と帰属表示を維持する。Wikipedia・Commons の外部ページや UNESCO の一般の写真のライセンスを一括して推定しない。
