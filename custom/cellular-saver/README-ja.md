# Twitter-Piko モバイル通信限定データセーバー

対象: 添付された com.twitter.android / 12.19.1-release.0 / versionCode 2147483647。
ビルドとAPK署名の検証は完了しています。Galaxy S23上の起動・通信切替・ログイン維持は未検証のため、実機確認用です。

## すぐ使う

1. 元のアプリをアンインストールせず、生成した twitter-piko-12.19.1-cellular-saver.apk を更新インストールします。
2. 起動してログインが残っていることを確認します。
3. Wi-FiでデータセーバーOFF → Wi-Fiを切ってモバイル通信でON → Wi-Fiに戻してOFFを確認します。設定画面は開き直してください。
4. Wi-Fi時に画像画質・動画自動再生が元の設定へ戻ることも確認します。

ADBの場合:

```sh
adb install -r twitter-piko-12.19.1-cellular-saver.apk
```

署名、パッケージ名、versionCodeが元APKと一致しているため、通常の更新インストールとしてデータを保持できます。セッションの有効性はサーバーやアプリにも依存し、再ログイン不要を保証するものではありません。アンインストールやデータ消去は不要です。

## Morpheで自分でパッチする

現在のMorphe設定と署名鍵を先にエクスポートして保管してください。他アプリを自パッチしている場合、その更新には元の署名鍵が必要です。

1. Morpheの署名鍵インポートで、配布元の ks_pkcs12.keystore（以下のコマンドでは twitter-piko-signing.keystore として保存） を読み込みます。
   - ストアパスワード: 123456789
   - エイリアス: jhc
   - 鍵パスワード: 123456789
   - 公開配布元で使われている既存の鍵です。新しく生成した鍵ではありません。
2. cellular-saver-12.19.1.mpp をMorpheで開き、ローカルパッチソースとして追加します。
3. Expert modeで、入力には今回あなたが添付した元の twitter-piko-v12.19.1-release.0.apk を指定します。
4. 今回は追加パッチ「Cellular-only data saver」だけを選択します。元APKにはPikoが適用済みなので、Piko全体を重ねて適用しません。
5. 生成したAPKを更新インストールします。

MPPにはJVMクラスとAndroid用classes.dexの両方を収録しています。Morphe Desktop 1.15.0 / Patcher 1.12.0で読込・適用確認済みです。端末版MorpheでのMPP読込は実機未検証です。Patcherのバージョン互換エラーが出た場合は強制続行せず、エラー全文を確認してください。完成APKの更新インストールはMorpheのMPP読込とは独立しています。

移行後、動作を確認してからObtainiumのこのアプリの更新監視を停止するか、管理対象から外してください。アプリ本体は削除しません。配布元の通常APKで更新すると今回の追加処理が消えます。

未改造の同バージョンを元にPiko全体から作り直す場合は、Pikoとこの追加パッチを併用し、Pikoの「Change version code」を2147483647に設定し、同じ署名鍵を使用します。この同時適用経路は今回未検証です。まず上記の添付APKへの追加適用経路を使ってください。

このMPPは12.19.1-release.0専用です。将来のアプリバージョンには内部クラスの確認とパッチ更新が必要です。バージョンを強制して適用しないでください。

## 動作

- 起動完了時と、アプリにとってのデフォルトネットワークの切替・能力変更時に判定します。
- 携帯回線でON。Wi-Fi、未接続、その他でOFF。テザリング先へのWi-Fi接続もOFFです。
- Wi-Fiと携帯回線の両方の属性が通知された場合はWi-Fiを優先します。
- 従量制Wi-Fiかどうかでは判定しません。
- VPNではOSがデフォルトネットワークに通知する接続種別に依存します。携帯回線属性が通知されないVPNではONになりません。
- アプリが停止中は動作せず、次回起動で反映します。常駐サービスやポーリング、root、Shizukuは使いません。
- ネイティブのデータセーバー処理で、画質・自動再生・アップロード品質・データ同期の設定を退避/復元します。単に表示スイッチだけを変更するものではありません。
- ネイティブの衛星回線向け自動制御は、この接続判定と競合しないよう停止しています。
- 手動でスイッチを変えた場合、次の接続変更または起動時に自動判定で上書きされます。常時強制ロックはしていません。

## 戻す

添付した元APKを `adb install -r` で上書きすると追加処理を除去できます。同じパッケージ、署名、versionCodeなのでデータ消去を伴うダウングレードは不要です。元APKへ戻した後、データセーバーのスイッチがONのままであれば手動でOFFにしてください。自動処理が変更した設定は単にコードを戻すだけでは自動で復元されません。

起動エラーや切替失敗があれば次のログを取得してください。

```sh
adb logcat -d -s CellularSaver AndroidRuntime
```

ログにはアカウント情報などが含まれることがあるため、共有前に確認してください。

## 検証結果

- Android API 36を指定したApkVerifierによるAPK署名検証に合格。
- 元APKと出力APKの署名証明書SHA-256が一致:
  637c226c67aec0cdbc6f49cd476d5247f999122606286273e16233a913a088b4
- パッケージ、バージョン名、versionCodeが一致。
- DEXとMETA-INF以外の全エントリが元APKとバイト単位で一致。リソース・Manifest・ネイティブライブラリを維持。
- MPPの読込、追加パッチ適用、DEX再構築、署名に成功。
- 出力DEXを再読込して、起動フック・初期化・既存設定処理への呼出し・ネットワークコールバックを確認。
- 実機の起動、ART検証、ネットワーク切替、通信量削減率、ログインセッション維持は未検証。

## ソースからビルド

Python 3とJDK 21を用意し、次の公式配布物を取得します。

- https://github.com/MorpheApp/morphe-desktop/releases/tag/v1.15.0
  morphe-desktop-1.15.0-all.jar
- https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.3.37/r8-8.3.37.jar

```sh
python build.py --jdk /path/to/jdk21 --morphe /path/to/morphe-desktop-1.15.0-all.jar --r8 /path/to/r8-8.3.37.jar
```

CLIでの追加パッチ適用:

```sh
java -Xmx4g -jar morphe-desktop-1.15.0-all.jar patch twitter-piko-v12.19.1-release.0.apk -p dist/cellular-saver-12.19.1.mpp --bytecode-mode FULL --keystore twitter-piko-signing.keystore --keystore-password 123456789 --keystore-entry-alias jhc --keystore-entry-password 123456789 -o twitter-piko-12.19.1-cellular-saver.apk
```

src/CellularSaverPatch.java がMorphe用パッチ、smali/mobile/saver/CellularSaver.smali がアプリに挿入する処理です。src/Assemble.java がDEX組立用、src/VerifyApk.java と src/VerifyDex.java が今回の検証用です。成果物を作るスクリプトは外部へのアップロードや端末へのインストールを行いません。

## ログイン問題について

今回のパッチには認証処理の変更を含みません。同じ署名による上書きで既存データを残す方針です。Proton Passのパスキーによる回避方法の現行動作は未確認です。Piko公式READMEにはX 12.5.0-release.0以降でX-Shimなしにログイン可能とありますが、個別アカウントでの成功を保証するものではありません。

## 参照

- https://github.com/monsivamon/twitter-apk （署名鍵・配布元）
- https://github.com/crimera/piko （パッチ元・ログイン案内）
- https://github.com/MorpheApp/morphe-manager （ローカルパッチ・署名鍵インポート）
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/reference/android/net/ConnectivityManager

追加パッチのソースはGPL-3.0-only。ビルドツールや元アプリの権利は各権利者に帰属します。
