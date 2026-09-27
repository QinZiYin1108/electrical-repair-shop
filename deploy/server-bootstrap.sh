#!/usr/bin/env bash
# Debian 13 (trixie) 首次环境准备（幂等，可重复执行）。需 root/sudo。
# 安装：OpenJDK 25 / Node 20 / MariaDB / Redis / Nginx / certbot / rsync，并创建目录、swap、systemd 服务。
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/electrical-repair-shop}"
SRC_DIR="$DEPLOY_ROOT/src"

if ! command -v apt-get >/dev/null 2>&1; then
  echo "本脚本面向 Debian/Ubuntu（apt）；当前系统未检测到 apt，请手动安装所需组件。" >&2
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update

echo "==> 基础组件（curl/tar/rsync/git/nginx/redis/certbot）"
apt-get install -y curl tar rsync git nginx redis-server certbot python3-certbot-nginx ca-certificates gnupg

echo "==> OpenJDK 25（Debian 13 主源自带；缺失则回退 Adoptium）"
if ! java -version 2>&1 | grep -q '"25'; then
  if ! apt-get install -y openjdk-25-jdk; then
    echo "    主源无 openjdk-25-jdk，改用 Adoptium 源"
    mkdir -p /etc/apt/keyrings
    wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public \
      | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
    . /etc/os-release
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${VERSION_CODENAME} main" \
      > /etc/apt/sources.list.d/adoptium.list
    apt-get update
    apt-get install -y temurin-25-jdk
  fi
fi

echo "==> Node.js（Debian 13 主源自带 20；缺失则用 NodeSource 20.x）"
if ! command -v node >/dev/null 2>&1; then
  if ! apt-get install -y nodejs npm; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
    apt-get install -y nodejs
  fi
fi

echo "==> 数据库：Oracle MySQL 8.4 LTS（官方源；注意 2025-10 旧签名 key 已过期）"
if ! command -v mysqld >/dev/null 2>&1; then
  apt-get install -y wget curl gnupg ca-certificates
  # 清理上次可能写入的过期 key / 源
  rm -f /etc/apt/keyrings/mysql.gpg /etc/apt/trusted.gpg.d/mysql.gpg /etc/apt/sources.list.d/mysql.list

  # 方案：官方 mysql-apt-config（内含刷新后的签名 key）；非交互选择 mysql-8.4-lts
  MYSQL_CFG=/tmp/mysql-apt-config.deb
  got=0
  for v in 0.8.36-1 0.8.35-1 0.8.34-1; do
    if curl -fsSL -o "$MYSQL_CFG" "https://dev.mysql.com/get/mysql-apt-config_${v}_all.deb"; then
      got=1
      break
    fi
  done
  if [ "$got" -eq 1 ]; then
    export DEBIAN_FRONTEND=noninteractive
    echo "mysql-apt-config mysql-apt-config/select-server select mysql-8.4-lts" | debconf-set-selections
    dpkg -i "$MYSQL_CFG" || true
    for p in /etc/apt/trusted.gpg.d/mysql.gpg /usr/share/keyrings/mysql.gpg /etc/apt/keyrings/mysql.gpg; do
      if [ -f "$p" ]; then mkdir -p /etc/apt/keyrings; cp -f "$p" /etc/apt/keyrings/mysql.gpg; break; fi
    done
  else
    echo "    未能下载 mysql-apt-config，稍后从 keyserver 获取刷新 key"
  fi

  . /etc/os-release
  echo "deb [signed-by=/etc/apt/keyrings/mysql.gpg] http://repo.mysql.com/apt/debian/ ${VERSION_CODENAME:-bookworm} mysql-8.4-lts mysql-tools" \
    > /etc/apt/sources.list.d/mysql.list

  # 刷新；若 key 仍不被接受，回退从 keyserver 拉取刷新后的 key
  if ! apt-get -o Acquire::http::Timeout=30 -o Acquire::Retries=3 update; then
    echo "    仍校验失败，回退从 keyserver 获取刷新 key"
    gpg --keyserver hkps://keyserver.ubuntu.com --recv-keys B7B3B788A8D3785C || true
    gpg --export B7B3B788A8D3785C > /etc/apt/keyrings/mysql.gpg || true
    chmod 644 /etc/apt/keyrings/mysql.gpg
    apt-get update
  fi

  # 非交互安装；root@localhost 默认 auth_socket（留空密码，用 sudo mysql 连接）
  debconf-set-selections <<< "mysql-community-server mysql-community-server/root-pass password "
  debconf-set-selections <<< "mysql-community-server mysql-community-server/re-root-pass password "
  apt-get install -y mysql-community-server || apt-get install -y mysql-server
fi
systemctl enable --now mysql 2>/dev/null || true

echo "==> 创建部署目录"
mkdir -p "$DEPLOY_ROOT/backend" "$DEPLOY_ROOT/official-website" "$DEPLOY_ROOT/admin-front-end"

echo "==> 检查 swap（2 核 4GB 构建防 OOM 建议至少 2G）"
if ! swapon --show 2>/dev/null | grep -q .; then
  echo "    未检测到 swap，创建 2G swapfile"
  fallocate -l 2G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "    已启用 swap（已写入 /etc/fstab）"
else
  echo "    已存在 swap，跳过创建"
fi

echo "==> 安装 systemd 服务"
UNIT_SRC="$SRC_DIR/deploy/systemd/electrical-backend.service"
if [ -f "$UNIT_SRC" ]; then
  cp "$UNIT_SRC" /etc/systemd/system/electrical-backend.service
  systemctl daemon-reload || true
  echo "    已安装 /etc/systemd/system/electrical-backend.service"
else
  echo "    未找到 $UNIT_SRC（请先推送源码，再次执行本脚本）"
fi

echo
echo "==> 首次准备完成。数据库为 Oracle MySQL 8.4；客户端命令用 mysql（root 走 auth_socket，用 sudo mysql）："
echo "  1) 准备后端环境变量："
echo "     cp $SRC_DIR/deploy/env/backend.env.example $DEPLOY_ROOT/.env && vi $DEPLOY_ROOT/.env"
echo "  2) 初始化数据库（MySQL 8.4）："
echo "     sudo mysql -e \"CREATE DATABASE IF NOT EXISTS electrical_repair_shop DEFAULT CHARACTER SET utf8mb4;\""
echo "     sudo mysql -e \"CREATE USER IF NOT EXISTS 'app'@'localhost' IDENTIFIED BY 'your-db-password'; GRANT ALL PRIVILEGES ON electrical_repair_shop.* TO 'app'@'localhost'; FLUSH PRIVILEGES;\""
echo "     sudo mysql electrical_repair_shop < $SRC_DIR/main/sql/electrical_repair_shop_init.sql"
echo "     （.env 中 DB_USERNAME=app、DB_PASSWORD=your-db-password）"
echo "  3) systemctl enable --now electrical-backend"
echo "  4) Nginx 站点与证书：EMAIL=you@example.com bash $SRC_DIR/deploy/setup-nginx-ssl.sh"
echo "  5) 本地执行 deploy/push-deploy.ps1 进行首次部署"
