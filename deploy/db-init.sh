#!/usr/bin/env bash
# 用 .env 里的 DB_* 初始化数据库：建库 + 建/对齐应用用户 + 导入 init.sql。
# 保证 app 用户密码与 .env 完全一致（避免 Access denied）。
# 用法：bash deploy/db-init.sh
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/electrical-repair-shop}"
SRC_DIR="${SRC_DIR:-$DEPLOY_ROOT/src}"
ENV_FILE="$DEPLOY_ROOT/.env"
INIT_SQL="$SRC_DIR/main/sql/electrical_repair_shop_init.sql"

[ -f "$ENV_FILE" ] || { echo "缺少 $ENV_FILE（先按 GO-LIVE 步骤3 生成）" >&2; exit 1; }
[ -f "$INIT_SQL" ] || { echo "缺少 $INIT_SQL" >&2; exit 1; }

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

: "${DB_USERNAME:?请在 .env 设置 DB_USERNAME}"
: "${DB_PASSWORD:?请在 .env 设置 DB_PASSWORD}"
DB_HOST="$(echo "${DB_URL:-}" | sed -E 's#^jdbc:mysql://([^:/?]+).*#\1#')"
DB_NAME="$(echo "${DB_URL:-}" | sed -E 's#.*/([^?]+).*#\1#')"
DB_NAME="${DB_NAME:-electrical_repair_shop}"

echo "==> 初始化数据库 host=${DB_HOST:-127.0.0.1} db=${DB_NAME} user=${DB_USERNAME}"
mysql -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4;"
mysql -e "CREATE USER IF NOT EXISTS '$DB_USERNAME'@'localhost' IDENTIFIED BY '$DB_PASSWORD';"
# 显式对齐密码（关键：保证与 .env 一致）
mysql -e "ALTER USER '$DB_USERNAME'@'localhost' IDENTIFIED BY '$DB_PASSWORD';"
mysql -e "GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USERNAME'@'localhost'; FLUSH PRIVILEGES;"

echo "==> 导入初始化脚本 $INIT_SQL"
mysql "$DB_NAME" < "$INIT_SQL"

echo "==> 完成。表数："
mysql "$DB_NAME" -e "SELECT COUNT(*) AS tables FROM information_schema.tables WHERE table_schema='$DB_NAME';"
echo "app 用户密码已与 .env 对齐，可重启后端：systemctl restart electrical-backend"
