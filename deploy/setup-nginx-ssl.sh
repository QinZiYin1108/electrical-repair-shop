#!/usr/bin/env bash
# 安装 Nginx 站点并申请 Let's Encrypt 证书（certbot）。需 root/sudo。
# 用法：EMAIL=you@example.com bash deploy/setup-nginx-ssl.sh
# 可选：DOMAINS="thdqwx.work www.thdqwx.work admin.thdqwx.work api.thdqwx.work"
set -euo pipefail

EMAIL="${EMAIL:-}"
DOMAINS="${DOMAINS:-thdqwx.work www.thdqwx.work admin.thdqwx.work api.thdqwx.work}"
CERT_NAME="${CERT_NAME:-thdqwx.work}"
SRC_DIR="${SRC_DIR:-/opt/electrical-repair-shop/src}"
CONF_SRC="$SRC_DIR/deploy/nginx/thdqwx.work.conf"
CONF_DST="/etc/nginx/conf.d/thdqwx.work.conf"
BOOTSTRAP_CONF="/etc/nginx/conf.d/zz-acme-bootstrap.conf"

if [ -z "$EMAIL" ]; then
  echo "请设置 EMAIL，例如：EMAIL=you@example.com bash deploy/setup-nginx-ssl.sh" >&2
  exit 1
fi

echo "==> 安装 nginx 与 certbot"
if command -v apt-get >/dev/null 2>&1; then
  apt-get update
  apt-get install -y nginx certbot python3-certbot-nginx
else
  dnf install -y nginx certbot python3-certbot-nginx || yum install -y nginx certbot python3-certbot-nginx || true
fi

echo "==> 部署临时 HTTP 引导站点（用于 ACME 校验）"
D=""
for d in $DOMAINS; do D="$D -d $d"; done
cat > "$BOOTSTRAP_CONF" <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAINS;
    location /.well-known/acme-challenge/ { root /var/www/html; }
    location / { return 200 'ok'; }
}
EOF
systemctl enable --now nginx || true
nginx -t && systemctl reload nginx

echo "==> 申请证书"
if [ -d "/etc/letsencrypt/live/$CERT_NAME" ]; then
  echo "    证书已存在，尝试续期"
  certbot renew --quiet || true
else
  certbot certonly --nginx --non-interactive --agree-tos -m "$EMAIL" $D
fi

echo "==> 部署正式站点配置"
rm -f "$BOOTSTRAP_CONF"
cp "$CONF_SRC" "$CONF_DST"
nginx -t
systemctl reload nginx

echo "==> 完成。"
echo "    访问： https://www.thdqwx.work  https://admin.thdqwx.work  https://api.thdqwx.work"
echo "    续期： certbot 已自动安装续期定时器（systemctl status certbot.timer）"
