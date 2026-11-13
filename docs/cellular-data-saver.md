# モバイル通信時のデータセーバー自動切り替え

このブランチは、Piko v3.11.1（2026-11-10公開）に、設定で有効・無効を切り替えられる `Cellular data saver` パッチを追加しています。

## 使い方

対応するXのバージョンは、公式版と同じ `12.19.1-release.0` です。

1. [このフォークのReleases](https://github.com/yangniao23/piko/releases)から `Piko ... (cellular data saver)` のMPPを取得し、Morpheに読み込みます。このブランチから自分でビルドすることもできます。
2. 対応する元のXのAPKMに、必要なPikoパッチと `Cellular data saver` を適用します。設定画面の追加パッチも依存関係として適用されます。
3. Xを起動し、Piko設定のその他の項目で「モバイル通信時のみデータセーバーを自動で有効にする」を有効にします。初期状態は無効です。

公式PikoのMPPには、この追加機能は含まれません。また、別ブランチ `cellular-data-saver` の単独MPPとは配布形式が異なります。

Morpheのソースとして登録する場合は [Piko + Cellular data saver を追加](https://morphe.software/add-source?github=yangniao23/piko) を使い、ソースを更新してください。選択するソースは `yangniao23/piko`、バンドル名は `Piko + Cellular data saver` です。`Cellular data saver` が一覧にない場合は、公式版のソースや古いバンドルを選んでいないか確認してください。

## 配布

`cellular-data-saver-settings` へのpushで、専用の `Release Piko with cellular data saver` ワークフローがビルドと文字列検査を実行し、成功した場合にGitHub ReleasesへMPPを公開します。公開後、既定ブランチの `patches-bundle.json` を更新し、Morpheのソース登録からも追加版を取得できるようにします。公式版の自動リリースとは独立しており、追加のGPGシークレットは使いません。

リリース名は `v3.11.1-cellular.<実行番号>.<試行番号>` の形式です。MPP、ダウンロードURLを記録した `patches-bundle.json`、`SHA256SUMS` が添付されます。MPPは署名なしで、端末へのインストールや実機検証は自動では行いません。

## 動作

- モバイル通信ではX標準のデータセーバーを有効にし、Wi-Fi・未接続・その他の回線では無効にします。Wi-Fiとモバイル通信の属性が同時に通知された場合はWi-Fiを優先します。
- 手動変更は、次の回線変更またはアプリのプロセス再起動まで維持します。
- 自動切り替えを無効にしても、その時点のデータセーバーの状態は維持します。
- VPN利用時の判定は、Androidから通知される回線属性に依存します。

## チャットの「更新が必要」への対応

`Spoof client version` も収録しています。[公式への提案 #2302](https://github.com/crimera/piko/pull/2302)（24bit192kHz氏）を元に、使用中のMorphe Patcher 2.3.0へ移植しました。公式にはまだ採用されていない修正です。

Morpheで `Spoof client version` を選択し、元の `12.19.1-release.0` のAPKMに必要なパッチを適用し直してください。報告バージョンは初期値の `12.41.0-prod.01`、バージョンコードは `312410001` のまま使います。MPPを更新するだけでは、インストール済みのXに修正は反映されません。

X Chatの認証・メッセージ送信などの通信に対し、新しいクライアントバージョンを申告します。通常のタイムラインなどの通信は元のバージョンを維持します。既存のインストールを更新する際は、これまでと同じ署名鍵を使ってください。

通信の振り分けはアカウントを使わない自動テストで確認します。このフォークでのAPK適用と実機のチャット復旧は未検証です。

## 今回の更新と検証

- 公式の `v3.11.1` タグを取り込み、Morphe Patcher 2系の `patch` API、移動したユーティリティのインポート、ネイティブブリッジのメソッド置換に対応しました。
- 対応Xバージョン、設定キー、初期値、自動切り替えの処理は維持しています。
- ビルドコマンドは `./gradlew buildAndroid :patches:checkStringResources` です。GitHub Packagesからの依存関係取得には、利用可能なGitHub認証が必要です。
- ローカルでは依存関係の取得がHTTP 401で失敗しましたが、[GitHub Actionsのビルドと文字列検査](https://github.com/yangniao23/piko/actions/runs/43397133017)は成功し、[v3.11.1-cellular.2.1](https://github.com/yangniao23/piko/releases/tag/v3.11.1-cellular.2.1)のMPPを公開済みです。APKへの適用と実機動作は未検証です。
