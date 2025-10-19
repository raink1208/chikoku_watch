#!/bin/bash

set -e

echo "統合ツール (WatchTool + DeployTool) 開始..."

# Cronスケジュールを分解する関数
parse_cron_schedule() {
    local schedule="$1"
    
    # "*/3 * * * *" のような形式をパース
    if [[ $schedule =~ ^\*/([0-9]+)\ \*\ \*\ \*\ \*$ ]]; then
        # 分間隔の場合
        echo $((${BASH_REMATCH[1]} * 60))
        return 0
    fi
    
    # "0 3 * * *" のような形式（毎日午前3時）は1日間隔として扱う
    if [[ $schedule =~ ^[0-9]+\ [0-9]+\ \*\ \*\ \*$ ]]; then
        echo 86400  # 1日 = 86400秒
        return 0
    fi
    
    # その他の複雑な形式の場合はデフォルト値
    echo 3600  # 1時間
}

# 実行モードの判定
case "$1" in
    "cron")
        echo "スケジュールモードで起動します..."
        
        if [ -z "$CRON_SCHEDULE" ]; then
            echo "エラー: CRON_SCHEDULE環境変数が設定されていません"
            exit 1
        fi
        
        echo "スケジュール設定: $CRON_SCHEDULE"
        
        # スケジュール間隔を計算
        INTERVAL=$(parse_cron_schedule "$CRON_SCHEDULE")
        echo "実行間隔: ${INTERVAL}秒"
        
        # 初回実行
        echo "$(date): 初回実行を開始します..."
        /app/run-integrated.sh >> /app/logs/integrated.log 2>&1 &
        
        # 定期実行ループ
        while true; do
            echo "$(date): 次回実行まで ${INTERVAL} 秒待機します..."
            sleep $INTERVAL
            
            echo "$(date): スケジュール実行を開始します..."
            /app/run-integrated.sh >> /app/logs/integrated.log 2>&1 &
        done
        ;;
    "manual"|"run-once")
        echo "手動実行モードで起動します..."
        exec /app/run-integrated.sh
        ;;
    "shell"|"bash")
        echo "シェルモードで起動します..."
        exec /bin/bash
        ;;
    *)
        echo "使用法: $0 [cron|manual|run-once|shell|bash]"
        echo ""
        echo "モード:"
        echo "  cron      - スケジュール定期実行"
        echo "  manual    - 一度だけ手動実行"
        echo "  run-once  - 一度だけ手動実行（manualと同じ）"
        echo "  shell     - シェルモードで起動（デバッグ用）"
        echo "  bash      - シェルモードで起動（デバッグ用）"
        echo ""
        echo "環境変数:"
        echo "  CRON_SCHEDULE       - 実行スケジュール（例: '*/3 * * * *', '0 3 * * *'）"
        echo "  CHANNEL_ID          - YouTubeチャンネルID（必須）"
        echo "  YOUTUBE_API_KEY     - YouTube API キー（必須）"
        echo "  CLOUDFLARE_ACCOUNT_ID    - Cloudflareアカウントid（必須）"
        echo "  R2_ACCESS_KEY_ID         - R2アクセスキーID（必須）"
        echo "  R2_SECRET_ACCESS_KEY     - R2シークレットキー（必須）"
        echo "  BUCKET_NAME         - R2バケット名（デフォルト: youtube-analysis-data）"
        echo "  REMOTE_PATH         - アップロード先パス（デフォルト: youtube-analysis/result.json）"
        echo "  OUTPUT_FORMAT       - 出力形式（デフォルト: json）"
        echo "  EVENT_TYPE          - 配信タイプ（completed/live/upcoming）"
        exit 1
        ;;
esac