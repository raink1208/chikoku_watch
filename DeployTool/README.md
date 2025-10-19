# DeployTool - ファイル監視・自動アップロードツール

特定のファイルパスを監視し、ファイルの変更を検知してCloudflare R2ストレージなどのクラウドストレージに自動的にアップロードするツールです。

## 特徴

- **差分検知**: SHA-256ハッシュ値による変更検知で、変更されたファイルのみアップロード
- **リトライ機能**: ネットワークエラー時の自動リトライ（エクスポネンシャルバックオフ）
- **並列アップロード**: 複数ファイルの並列アップロードに対応
- **拡張可能**: 将来的に他のクラウドストレージにも対応可能な設計

## 必要要件

- Java 21以上
- メモリ: 最低256MB、推奨512MB

## セットアップ

### 1. ビルド

```bash
# Gradleラッパーを使用してビルド
gradlew build

# 実行可能JARの作成
gradlew jar
```

### 2. 設定ファイルの作成

`config.properties.example`をコピーして`config.properties`を作成します。

```bash
copy config.properties.example config.properties
```

### 3. 環境変数の設定

以下の環境変数を設定してください：

```bash
# Windows (PowerShell)
$env:CLOUDFLARE_ACCOUNT_ID="your-account-id"
$env:R2_ACCESS_KEY_ID="your-access-key-id"
$env:R2_SECRET_ACCESS_KEY="your-secret-access-key"

# Windows (Command Prompt)
set CLOUDFLARE_ACCOUNT_ID=your-account-id
set R2_ACCESS_KEY_ID=your-access-key-id
set R2_SECRET_ACCESS_KEY=your-secret-access-key
```

### 4. 設定ファイルの編集

`config.properties`を編集して、アップロード対象のファイルを設定します：

```properties
# アップロード対象ファイルの数
files.count=2

# ファイル1
files.0.local_path=C:/path/to/file1.txt
files.0.remote_path=uploads/file1.txt
files.0.enabled=true

# ファイル2
files.1.local_path=C:/path/to/file2.json
files.1.remote_path=data/file2.json
files.1.enabled=true

# バケット名を設定
storage.cloudflare_r2.bucket_name=your-bucket-name
```

## 使い方

### 基本的な使用方法

```bash
# 通常実行（デフォルト設定ファイル使用）
java -jar build/libs/DeployTool-1.0-SNAPSHOT.jar

# 設定ファイルを指定
java -jar build/libs/DeployTool-1.0-SNAPSHOT.jar --config /path/to/config.properties
```

### オプション

| オプション | 短縮形 | 説明 |
|----------|--------|------|
| `--config` | `-c` | 設定ファイルのパス（デフォルト: `config.properties`） |
| `--dry-run` | `-n` | ドライラン実行（アップロードせず変更検知のみ） |
| `--force` | `-f` | 強制アップロード（変更検知をスキップ） |
| `--file` | - | 特定ファイルのみ処理 |
| `--verbose` | `-v` | 詳細ログ出力 |
| `--version` | `-V` | バージョン表示 |
| `--help` | `-h` | ヘルプ表示 |

### 使用例

```bash
# ドライラン（実際にはアップロードしない）
java -jar build/libs/DeployTool-1.0-SNAPSHOT.jar --dry-run

# 強制アップロード（変更がなくてもアップロード）
java -jar build/libs/DeployTool-1.0-SNAPSHOT.jar --force

# 特定ファイルのみ処理
java -jar build/libs/DeployTool-1.0-SNAPSHOT.jar --file C:/path/to/file1.txt

# ヘルプ表示
java -jar build/libs/DeployTool-1.0-SNAPSHOT.jar --help
```

## 定期実行の設定

ツール自体は一度だけ実行されます。定期実行が必要な場合は、Windowsタスクスケジューラを使用してください。

### Windowsタスクスケジューラの設定例

1. タスクスケジューラを開く
2. 「基本タスクの作成」を選択
3. トリガーを設定（例：5分ごと）
4. 操作で以下を設定：
   - プログラム: `java`
   - 引数: `-jar C:\path\to\DeployTool-1.0-SNAPSHOT.jar`
   - 作業フォルダー: `C:\path\to\`

## ログ

ログは以下の場所に出力されます：

- コンソール出力
- ファイル出力: `logs/deploy-tool.log`

ログレベルは`config.properties`で設定できます（DEBUG, INFO, WARN, ERROR）。

## 状態管理

前回実行時のファイル状態は`.deploy-state.json`に保存されます。このファイルは自動的に作成・更新されます。

状態ファイルの例：

```json
{
  "version": "1.0",
  "last_updated": "2025-10-19T02:17:00Z",
  "files": {
    "C:/path/to/file1.txt": {
      "hash": "sha256:a1b2c3d4e5f6...",
      "last_upload": "2025-10-19T02:15:00Z",
      "size": 1024,
      "status": "success"
    }
  }
}
```

## トラブルシューティング

### ファイルが見つからないエラー

- ファイルパスが正しいか確認してください
- 絶対パスを使用することを推奨します

### 認証エラー

- 環境変数が正しく設定されているか確認してください
- アクセスキーとシークレットキーが正しいか確認してください

### アップロードエラー

- バケット名が正しいか確認してください
- エンドポイントURLが正しいか確認してください
- ネットワーク接続を確認してください

### ログで詳細を確認

```bash
# 詳細ログを有効にして実行
java -jar build/libs/DeployTool-1.0-SNAPSHOT.jar --verbose
```

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 作者

rain1208

## バージョン履歴

- 1.0-SNAPSHOT: 初期リリース
  - Cloudflare R2対応
  - SHA-256ハッシュによる変更検知
  - リトライ機能
  - 並列アップロード

