# 独自変更の記録と公式リリースへの追従

基準は **Piko v3.11.1 / `297519632c897d19e9f0df0c39e5b315d1a7390b`**。対応確認の対象は **X 12.19.1-release.0 / Morphe Patcher 2.3.0**。基準、対応版、変更対象ファイルは `upstream.json` にまとめています。X-Shimへの依存はありません。

## 独自変更を小さく保つ構成

| 差分 | 内容 | 公式側で触るファイル |
|---|---|---|
| `01-cellular-data-saver.patch` | モバイル通信時のデータセーバー。初期値OFF、Piko設定で切り替え可能 | 設定3ファイルと日英の文字列。実装2ファイルは新規追加 |
| `02-xchat-client-version.patch` | チャットの更新要求回避。送信操作も対象とし、通常通信のバージョンは維持 | 実装2ファイルの新規追加のみ |
| `03-fork-identity.patch` | Morpheで公式版と追加版を区別する名前・URL | `patches/build.gradle.kts` の表示情報のみ |
| `04-release-tests-and-docs.patch` | 専用リリース、検査、変更記録、保守ツール | README以外は独自ファイルの追加 |

アプリ機能に必要な独自差分は先頭の2つです。元の公式パッチや依存ライブラリを独自に書き換えず、配布と検証の変更を別にしています。通常の利用手順は [データセーバーとチャットのガイド](../docs/cellular-data-saver.md) にあります。

## 今回の変更履歴

| 段階 | 変更と理由 | 記録 |
|---|---|---|
| 公式版への追従 | v3.9.0からv3.11.1へ更新。設定キーと初期値を維持 | `d45ee86a` |
| Patcher 2対応 | `patch` APIとユーティリティの移動に対応。読み取り専用になったメソッド実装はメソッド全体の置換で対応 | `9fef348c` |
| 配布 | 独自ブランチへのpushからリリース。名前を区別し、既定ブランチのMorpheメタデータを公開済みMPPへ更新 | `4fb93197`、`65831691` |
| チャット | [公式へのPR #2302](https://github.com/crimera/piko/pull/2302)、取り込み元 `5bfa1017` を移植。取り込み時点では公式未採用 | `314e8a41` |
| データセーバーの適用失敗 | `data_saved_toggled_by_sat_mode` が2種類のコールバックに存在。別機能を選んだため、4.1で `CellularDataSaverPatch.kt:197` の例外が発生 | 4.1のMPPで同じ例外を再現 |
| 適用失敗の修正 | 管理クラスへの参照でコールバックを限定。設定APIはネイティブ切り替え処理から取得。チャット側は既存のPlay Store更新抑止フックにも対応 | `3012d351` |
| 検証済みリリース | 両追加パッチの適用、APK再生成、DEX再読み込みに成功。検査用CLIはJava 21、ビルドはJava 17 | [6.1](https://github.com/yangniao23/piko/releases/tag/v3.11.1-cellular.6.1)、[Actions](https://github.com/yangniao23/piko/actions/runs/43474592669)、`9b2d38cf` |

## 差分の書き出し

このブランチのコミット済みの内容から、公式版との差分だけを生成します。実行にはGitとPython 3が必要です。作業中の編集、未追跡ファイル、APK、署名鍵は含みません。既存の未追跡ファイル `piko-cellular-saver.patch` は旧単独版の記録なので、今回の差分として使いません。

```sh
git fetch https://github.com/crimera/piko.git tag v3.11.1 --no-tags
python3 maintenance/export_patches.py --output /tmp/piko-custom-patches --verify
```

この取得は、フォーク側に公式タグがない場合に必要です。Actionsでも記録した公式タグを取得し、タグのコミットが `upstream.json` と一致することを検査します。

出力は4つのパッチ、適用順を記した `series`、基準コミット・作成元コミット・各パッチのSHA-256を記した `manifest.json` です。既存の出力先は上書きしません。`--verify` は一時的な公式版の作業コピーに差分を当て直し、コミット済みソースとファイル内容・ツリー全体が一致することを検査します。元のブランチや作業中の編集は変更しません。

機能の差分だけが必要なら、次のように絞れます。

```sh
python3 maintenance/export_patches.py --groups cellular,chat --output /tmp/piko-features --verify
```

差分を記録した基準と実際の変更対象がずれた場合や、X・Patcherの対応版が変わった場合は書き出しを止めます。古い検査対象のまま新しい版に対応済みと扱わないためです。

## 次の公式リリースへ追従する

通常は現在の `cellular-data-saver-settings` ブランチに公式タグをマージします。新規追加の実装は維持されるため、競合を調整する主な対象は上表の少数の接続部分です。

1. 作業中の変更を保存し、新しい公式タグを取得します。`git fetch https://github.com/crimera/piko.git tag vX.Y.Z --no-tags` の `vX.Y.Z` は実際のリリース名に置き換えます。
2. `git merge --no-commit vX.Y.Z` で取り込みます。競合時は公式の更新を維持し、必要な設定の接続部分だけを残します。
3. 公式側にデータセーバーやチャット修正が入っていないか確認します。特にPR #2302または同等機能が採用された場合は、独自のチャット実装を重ねず、重複を除去します。関連テストとパッチ一覧の検査も合わせて調整します。
4. `maintenance/upstream.json` の `tag` と `commit` を取得した公式タグへ更新します。
5. Xが同じ12.19.1でPatcherのAPIも同じなら、独自機能のコード変更は原則不要です。それでも下の検査を実行します。
6. Xの対象版またはPatcherが変わった場合は、ネイティブ処理とAPIを調査してから `verified_x_versions` / `verified_patcher_version` を更新します。APK検査用の配布元・バージョン・SHA-256、`VerifyCellularPatch.java` の検査、リリース説明の対象版も一緒に見直します。互換性の宣言だけを広げません。
7. 更新をコミットし、差分の書き出しと再適用検査を実行します。push後のActionsが全検査に成功すると、MPPとソース追従用アーカイブが公開されます。

### 新しい公式ソースへ差分だけ移す場合

まず現在のブランチから上記の4つのパッチを書き出し、別のクリーンな公式作業コピーで次を実行します。

```sh
git apply --check --index /tmp/piko-custom-patches/*.patch
git apply --index /tmp/piko-custom-patches/*.patch
```

新しい公式版で文脈が変わっていれば、最初のコマンドが止まります。`--reject` やバージョン強制で進めず、該当する小さな差分を調整します。適用後は上記の手順3以降を行い、基準を新しい公式タグへ更新してからコミットします。公式採用済みの機能は対応する差分を省きます。

## 公開前に確認すること

- 差分を書き出して公式基準へ当て直せること。全差分なら元のコミットのツリーと完全一致すること。
- `python3 .github/scripts/check_chat_version_routing.py` が成功すること。
- `./gradlew buildAndroid :patches:checkStringResources :patches:generatePatchesList` が成功すること。GitHub Packagesの認証が必要です。
- `Cellular data saver` と `Spoof client version` がパッチ一覧に1つずつ存在すること。公式採用時は一覧とテストを実際の構成に合わせます。
- 実APK検査で両追加パッチの適用・APK再生成が成功し、正しいコールバックだけの変更、ネイティブ呼び出し、起動処理、チャット操作リストが出力DEXに存在すること。
- リリースのダウンロードURLと、既定ブランチの `patches-bundle.json` が一致すること。

現在のAPK検査は、固定した公開済みPiko APKに追加の2機能を適用する検査です。未パッチAPKMへの全75項目の適用、Galaxy SC-51Dでの起動、回線切り替え、DMの送受信・ログイン維持は別途実機確認が必要です。
