#!/bin/bash
# 統合実行スクリプト: WatchTool -> DeployTool の順次実行

set -e

echo "$(date): 統合分析・デプロイ処理を開始します..."

# 環境変数のチェック
check_env_vars() {
    local missing_vars=()
    
    if [ -z "$CHANNEL_ID" ]; then
        missing_vars+=("CHANNEL_ID")
    fi
    
    if [ -z "$YOUTUBE_API_KEY" ]; then
        missing_vars+=("YOUTUBE_API_KEY")
    fi
    
    if [ -z "$CLOUDFLARE_ACCOUNT_ID" ]; then
        missing_vars+=("CLOUDFLARE_ACCOUNT_ID")
    fi
    
    if [ -z "$R2_ACCESS_KEY_ID" ]; then
        missing_vars+=("R2_ACCESS_KEY_ID")
    fi
    
    if [ -z "$R2_SECRET_ACCESS_KEY" ]; then
        missing_vars+=("R2_SECRET_ACCESS_KEY")
    fi
    
    if [ ${#missing_vars[@]} -gt 0 ]; then
        echo "エラー: 以下の環境変数が設定されていません: ${missing_vars[*]}"
        exit 1
    fi
}

# 環境変数チェック
check_env_vars

# 出力ディレクトリの作成
mkdir -p /app/output /app/logs

# ===== STEP 1: WatchTool実行 =====
echo "$(date): STEP 1 - WatchTool実行開始"
echo "$(date): チャンネルID: $CHANNEL_ID"

# WatchTool用の引数を構築
watchtool_args=""

if [ ! -z "$CHANNEL_ID" ]; then
    watchtool_args="$watchtool_args --channel $CHANNEL_ID"
fi

if [ ! -z "$OUTPUT_FORMAT" ]; then
    watchtool_args="$watchtool_args --format $OUTPUT_FORMAT"
fi

if [ ! -z "$OUTPUT_PATH" ]; then
    watchtool_args="$watchtool_args --output $OUTPUT_PATH"
fi

# WatchTool実行
echo "$(date): 実行コマンド: java -jar /app/watchtool.jar $watchtool_args"

if java -jar /app/watchtool.jar $watchtool_args; then
    echo "$(date): WatchTool実行完了"
    
    # 出力ファイルの確認
    if [ -f "$OUTPUT_PATH" ]; then
        echo "$(date): 出力ファイル生成確認: $OUTPUT_PATH"
        echo "$(date): ファイルサイズ: $(wc -c < "$OUTPUT_PATH") bytes"
    else
        echo "$(date): エラー: 出力ファイルが生成されませんでした: $OUTPUT_PATH"
        exit 1
    fi
else
    echo "$(date): エラー: WatchTool実行中にエラーが発生しました"
    exit 1
fi

# ===== STEP 2: DeployTool実行 =====
echo "$(date): STEP 2 - DeployTool実行開始"

# DeployTool設定ファイルを動的に生成
cat > /app/config/deploy-runtime.properties << EOF
# Runtime generated config for DeployTool
state.file=/app/.deploy-state.json

files.count=1

# WatchToolの出力ファイルをアップロード
files.0.local_path=$OUTPUT_PATH
files.0.remote_path=$REMOTE_PATH
files.0.enabled=true

storage.provider=cloudflare-r2

# Cloudflare R2設定
storage.cloudflare_r2.account_id=$CLOUDFLARE_ACCOUNT_ID
storage.cloudflare_r2.access_key_id=$R2_ACCESS_KEY_ID
storage.cloudflare_r2.secret_access_key=$R2_SECRET_ACCESS_KEY
storage.cloudflare_r2.bucket_name=$BUCKET_NAME
storage.cloudflare_r2.endpoint=https://$CLOUDFLARE_ACCOUNT_ID.r2.cloudflarestorage.com

# ログ設定
logging.level=INFO
logging.file=/app/logs/deploy-tool.log
logging.console=true

# リトライ設定
retry.max_attempts=3
retry.initial_delay_ms=1000
retry.max_delay_ms=10000
retry.multiplier=2.0
EOF

echo "$(date): DeployTool設定ファイルを生成しました"

# DeployTool実行
if java -jar /app/deploytool.jar --config /app/config/deploy-runtime.properties; then
    echo "$(date): DeployTool実行完了"
    echo "$(date): ファイルアップロード成功: $OUTPUT_PATH -> $REMOTE_PATH"
else
    echo "$(date): 警告: DeployTool実行中にエラーが発生しました"
    # DeployToolのエラーでは全体を失敗させない（WatchToolは成功しているため）
fi

echo "$(date): 統合処理完了"
echo "$(date): ====================================="