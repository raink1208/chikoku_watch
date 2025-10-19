# YouTube配信集計ツール (WatchTool)

YouTube Data API v3を使用して、特定のチャンネルの配信情報を取得し、遅刻配信の統計を分析するツールです。

## 機能

- YouTubeチャンネルの全配信情報を取得
- 配信の予定開始時刻と実際の開始時刻を比較
- 遅刻配信の検出と統計計算
- JSON、CSV、テキストレポート形式での出力
- 月別・曜日別・時間帯別の遅刻分析

## セットアップ

### 1. 必要要件

- JDK 21以上
- YouTube Data API v3のAPIキー

### 2. APIキーの取得

1. [Google Cloud Console](https://console.cloud.google.com/)にアクセス
2. 新規プロジェクトを作成
3. YouTube Data API v3を有効化
4. 認証情報からAPIキーを作成

### 3. 設定ファイルの作成

`config/api_config.properties`ファイルを作成し、APIキーを設定します：

```properties
youtube.api.key=YOUR_API_KEY_HERE
youtube.api.quota.limit=10000
fetch.max.results=50
fetch.max.videos=1000
delay.threshold.minutes=1
```

または、環境変数`YOUTUBE_API_KEY`にAPIキーを設定することもできます。

### 4. ビルド

```bash
gradlew build
```

## 使用方法

### 基本的な使用

```bash
java -jar build\libs\WatchTool-1.0-SNAPSHOT.jar -c チャンネルID
```

### オプション

- `-c, --channel <CHANNEL_ID>` : 対象チャンネルID（必須）
- `-o, --output <PATH>` : 出力ファイルパス（デフォルト: output/result.json）
- `-f, --format <FORMAT>` : 出力形式（json/csv/text、デフォルト: json）
- `-l, --limit <NUM>` : 取得する配信の最大数（デフォルト: 全件）
- `-s, --start-date <DATE>` : 取得開始日（YYYY-MM-DD形式）
- `-e, --end-date <DATE>` : 取得終了日（YYYY-MM-DD形式）
- `--event-type <TYPE>` : 配信タイプ（completed/live/upcoming、デフォルト: completed）
- `-v, --verbose` : 詳細ログ出力
- `-h, --help` : ヘルプ表示

### 使用例

#### JSON形式で出力

```bash
java -jar build\libs\WatchTool-1.0-SNAPSHOT.jar -c UCxxxxx -f json -o output\result.json
```

#### CSV形式で出力

```bash
java -jar build\libs\WatchTool-1.0-SNAPSHOT.jar -c UCxxxxx -f csv -o output\result.csv
```

#### テキストレポートで出力

```bash
java -jar build\libs\WatchTool-1.0-SNAPSHOT.jar -c UCxxxxx -f text -o output\report.txt
```

#### 期間を指定して取得

```bash
java -jar build\libs\WatchTool-1.0-SNAPSHOT.jar -c UCxxxxx -s 2025-01-01 -e 2025-03-31
```

#### 最大100件まで取得

```bash
java -jar build\libs\WatchTool-1.0-SNAPSHOT.jar -c UCxxxxx -l 100
```

## 出力形式

### JSON形式

```json
{
  "channelInfo": {
    "channelId": "UCxxxxx",
    "channelName": "チャンネル名",
    "subscriberCount": 100000
  },
  "statistics": {
    "totalStreams": 100,
    "delayedStreams": 45,
    "delayRate": 45.0,
    "averageDelayMinutes": 8.5
  },
  "streams": [...]
}
```

### CSV形式

動画ID、タイトル、予定開始時刻、実際の開始時刻、遅刻時間、視聴回数などをCSV形式で出力します。

### テキストレポート形式

読みやすいテキスト形式で統計サマリーと詳細分析を出力します。

## 遅刻分類

- **定刻開始**: 0分
- **軽微な遅刻**: 1-5分
- **通常遅刻**: 6-15分
- **大幅遅刻**: 16-30分
- **深刻な遅刻**: 31分以上

## APIクォータについて

YouTube Data API v3には1日あたり10,000ユニットのクォータ制限があります。

- `channels.list`: 1ユニット
- `search.list`: 100ユニット
- `videos.list`: 1ユニット

大量の配信を取得する場合は、`-l`オプションで取得件数を制限することを推奨します。

## トラブルシューティング

### APIキーエラー

```
API key is invalid or quota exceeded
```

- APIキーが正しく設定されているか確認
- Google Cloud ConsoleでYouTube Data API v3が有効になっているか確認
- 1日のクォータ制限に達していないか確認

### チャンネルが見つからない

```
チャンネル情報を取得できませんでした
```

- チャンネルIDが正しいか確認
- チャンネルが非公開になっていないか確認

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 参考資料

- [YouTube Data API v3 公式ドキュメント](https://developers.google.com/youtube/v3/docs)
- [仕様書](docs/仕様書.md)
# YouTube API設定
youtube.api.key=YOUR_API_KEY_HERE
youtube.api.quota.limit=10000

# 取得設定
fetch.max.results=50
fetch.enable.pagination=true
fetch.max.videos=1000

# 遅刻判定設定
delay.threshold.minutes=1

