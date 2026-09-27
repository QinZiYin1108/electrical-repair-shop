#!/usr/bin/env bash
# 服务器端：从 Git 拉取最新代码后构建部署。适合只用 XShell（不依赖本地推送）的场景。
# 用法：bash deploy/server-pull-deploy.sh [--skip-backend|--skip-frontend]
# 可通过环境变量覆盖：SRC_DIR / REPO_URL / BRANCH
set -euo pipefail

SRC_DIR="${SRC_DIR:-/opt/electrical-repair-shop/src}"
REPO_URL="${REPO_URL:-https://github.com/QinZiYin1108/electrical-repair-shop.git}"
BRANCH="${BRANCH:-feat/new-thing}"

if [ ! -d "$SRC_DIR/.git" ]; then
  echo "==> 目录尚未初始化，首次 clone $BRANCH"
  mkdir -p "$(dirname "$SRC_DIR")"
  git clone -b "$BRANCH" "$REPO_URL" "$SRC_DIR"
else
  echo "==> 拉取最新代码（$BRANCH）"
  cd "$SRC_DIR"
  git fetch origin
  git checkout "$BRANCH"
  # 服务器作为部署目标，直接对齐远程（丢弃服务器上的本地改动）
  git reset --hard "origin/$BRANCH"
fi

exec bash "$SRC_DIR/deploy/build-deploy.sh" "$@"
