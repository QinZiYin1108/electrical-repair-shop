#!/usr/bin/env bash
# 服务器端：构建后端与前端、发布产物、重启后端。
# 用法：bash deploy/build-deploy.sh [--skip-backend] [--skip-frontend]
# 可通过环境变量覆盖：SRC_DIR / DEPLOY_ROOT / SERVICE_NAME
set -euo pipefail

# 小内存机器（如 2 核 4GB）构建内存上限，可用环境变量覆盖
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx1024m -XX:MaxMetaspaceSize=320m}"
export NODE_OPTIONS="${NODE_OPTIONS:---max-old-space-size=1024}"
# 国内 npm 镜像（可用环境变量覆盖）
export NPM_CONFIG_REGISTRY="${NPM_CONFIG_REGISTRY:-https://registry.npmmirror.com}"

SRC_DIR="${SRC_DIR:-/opt/electrical-repair-shop/src}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/electrical-repair-shop}"
BACKEND_DIR="$DEPLOY_ROOT/backend"
OFFICIAL_DIR="$DEPLOY_ROOT/official-website"
ADMIN_DIR="$DEPLOY_ROOT/admin-front-end"
SERVICE_NAME="${SERVICE_NAME:-electrical-backend}"

SKIP_BACKEND=0
SKIP_FRONTEND=0
for arg in "$@"; do
  case "$arg" in
    --skip-backend) SKIP_BACKEND=1 ;;
    --skip-frontend) SKIP_FRONTEND=1 ;;
    *) echo "未知参数: $arg" >&2; exit 1 ;;
  esac
done

# 载入 .env，提供 ADMIN_API_BASE_URL 等构建期变量
if [ -f "$DEPLOY_ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$DEPLOY_ROOT/.env"
  set +a
fi
ADMIN_API_BASE_URL="${ADMIN_API_BASE_URL:-/api}"

echo "==> 资源情况（构建前）"
free -h 2>/dev/null || true
echo "    MAVEN_OPTS=$MAVEN_OPTS"
echo "    NODE_OPTIONS=$NODE_OPTIONS"

# 可选：构建期间暂停占用内存的其它服务（如个人博客），构建结束自动恢复。
# 用法：PAUSE_SERVICES="blog.service" bash deploy/build-deploy.sh
PAUSE_SERVICES="${PAUSE_SERVICES:-}"
resume_services() {
  for svc in $PAUSE_SERVICES; do
    systemctl start "$svc" 2>/dev/null || true
  done
}
if [ -n "$PAUSE_SERVICES" ]; then
  trap resume_services EXIT
  for svc in $PAUSE_SERVICES; do
    echo "    构建期间暂停服务: $svc"
    systemctl stop "$svc" 2>/dev/null || true
  done
fi

publish_dist() {
  local src="$1" dest="$2"
  mkdir -p "$dest"
  if command -v rsync >/dev/null 2>&1; then
    rsync -a --delete "$src"/ "$dest"/
  else
    rm -rf "${dest:?}/"*
    cp -r "$src"/. "$dest"/
  fi
}

if [ "$SKIP_BACKEND" -eq 0 ]; then
  echo "==> 构建后端"
  cd "$SRC_DIR/back-end"
  chmod +x mvnw 2>/dev/null || true
  ./mvnw -q -DskipTests clean package
  JAR="$(ls -t target/back-end-*.jar 2>/dev/null | grep -v '\.original$' | head -1 || true)"
  if [ -z "$JAR" ]; then
    echo "未找到后端 jar 产物" >&2
    exit 1
  fi
  mkdir -p "$BACKEND_DIR"
  cp "$JAR" "$BACKEND_DIR/app.jar"
  echo "    后端产物: $JAR -> $BACKEND_DIR/app.jar"
fi

if [ "$SKIP_FRONTEND" -eq 0 ]; then
  echo "==> 构建官网"
  cd "$SRC_DIR/official-website"
  npm ci || npm install
  npm run build
  publish_dist "$SRC_DIR/official-website/dist" "$OFFICIAL_DIR"

  echo "==> 构建后台（API 基址: $ADMIN_API_BASE_URL）"
  cd "$SRC_DIR/admin-front-end"
  npm ci || npm install
  VUE_APP_API_BASE_URL="$ADMIN_API_BASE_URL" npm run build
  publish_dist "$SRC_DIR/admin-front-end/dist" "$ADMIN_DIR"
fi

echo "==> 重启后端"
if systemctl list-unit-files 2>/dev/null | grep -q "^${SERVICE_NAME}.service"; then
  systemctl restart "$SERVICE_NAME"
  echo "    已重启 $SERVICE_NAME"
else
  echo "    未找到 systemd 服务 $SERVICE_NAME，跳过重启（请先执行 deploy/server-bootstrap.sh）"
fi

echo "==> 部署完成"
