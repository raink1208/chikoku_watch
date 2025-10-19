FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /build

# WatchToolをビルド
COPY WatchTool/build.gradle.kts WatchTool/settings.gradle.kts WatchTool/gradle.properties ./WatchTool/
COPY WatchTool/gradle ./WatchTool/gradle
COPY WatchTool/src ./WatchTool/src

WORKDIR /build/WatchTool
RUN gradle build --no-daemon -x test

# DeployToolをビルド
WORKDIR /build
COPY DeployTool/build.gradle.kts DeployTool/settings.gradle.kts DeployTool/gradle.properties ./DeployTool/
COPY DeployTool/gradle ./DeployTool/gradle
COPY DeployTool/src ./DeployTool/src

WORKDIR /build/DeployTool
RUN gradle build --no-daemon -x test

# 実行用イメージ
FROM eclipse-temurin:21-jre-alpine

# 必要なパッケージをインストール
RUN apk add --no-cache tzdata bash

# タイムゾーンを設定
ENV TZ=Asia/Tokyo

WORKDIR /app

# ビルドしたJARファイルをコピー
COPY --from=builder /build/WatchTool/build/libs/*.jar /app/watchtool.jar
COPY --from=builder /build/DeployTool/build/libs/*.jar /app/deploytool.jar

# 設定ファイルとスクリプト用ディレクトリを作成
RUN mkdir -p /app/config /app/output /app/logs /var/log/cron

# 実行スクリプトを作成（ルートから直接参照）
COPY run-integrated.sh /app/run-integrated.sh
COPY entrypoint.sh /app/entrypoint.sh

# スクリプトに実行権限を付与
RUN chmod +x /app/run-integrated.sh /app/entrypoint.sh

# デフォルトの環境変数設定
ENV CRON_SCHEDULE="0 4 * * *"
ENV CHANNEL_ID=""
ENV YOUTUBE_API_KEY=""
ENV OUTPUT_FORMAT="json"
ENV OUTPUT_PATH="/app/output/result.json"
ENV EVENT_TYPE="completed"
ENV LIMIT=""
ENV CLOUDFLARE_ACCOUNT_ID=""
ENV R2_ACCESS_KEY_ID=""
ENV R2_SECRET_ACCESS_KEY=""
ENV BUCKET_NAME=""
ENV REMOTE_PATH=""

# ボリュームマウントポイント
VOLUME ["/app/config", "/app/output", "/app/logs"]

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["cron"]