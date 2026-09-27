#!/usr/bin/env bash
# 服务器端：解包上传的源码 tar，然后执行构建部署。
# 用法：bash deploy/remote-deploy.sh /tmp/app-src.tar.gz [--skip-backend] [--skip-frontend]
set -euo pipefail

TAR_PATH="${1:-/tmp/app-src.tar.gz}"
SRC_DIR="${SRC_DIR:-/opt/electrical-repair-shop/src}"

if [ ! -f "$TAR_PATH" ]; then
  echo "未找到源码包: $TAR_PATH" >&2
  exit 1
fi

mkdir -p "$SRC_DIR"
echo "==> 解包 $TAR_PATH -> $SRC_DIR"
tar xzf "$TAR_PATH" -C "$SRC_DIR"

shift || true
exec bash "$SRC_DIR/deploy/build-deploy.sh" "$@"
