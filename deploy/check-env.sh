#!/usr/bin/env bash
# ==========================================================
# 部署自检脚本 — 校验 .env 外部依赖配置 + 服务连通性
# 用法：bash deploy/check-env.sh [/opt/electrical-repair-shop/.env]
# 退出码：0=无致命问题；非0=有 FAIL
# 说明：只读取 KEY=VALUE，不 source，避免密码特殊字符被解释
# ==========================================================
set -uo pipefail

ENV_FILE="${1:-/opt/electrical-repair-shop/.env}"
pass=0; warn=0; fail=0
ok(){ printf '  \033[32m[ OK ]\033[0m %s\n' "$1"; pass=$((pass+1)); }
wa(){ printf '  \033[33m[WARN]\033[0m %s\n' "$1"; warn=$((warn+1)); }
no(){ printf '  \033[31m[FAIL]\033[0m %s\n' "$1"; fail=$((fail+1)); }

if [ ! -f "$ENV_FILE" ]; then echo "找不到 $ENV_FILE"; exit 2; fi

getval(){ grep -E "^[[:space:]]*$1=" "$ENV_FILE" | tail -n1 | cut -d= -f2-; }

# 占位/示例值判定（与后端 StartupConfigurationValidator 的口径一致，并额外覆盖示例）
is_placeholder(){
  local v="$1"
  [ -z "$v" ] && return 1
  case "$v" in
    *change-me*|your*|*your-bucket*|*example.com*|*xxx*|*XXXX*|*xxxx*|wx0000000000000000) return 0 ;;
    '<'*'>') return 0 ;;
    *'<'*'>'*) return 0 ;;
  esac
  return 1
}

echo "==================== 部署自检: $ENV_FILE ===================="

echo "[1] 必填项（Spring 无默认值，缺失会导致启动失败）"
REQUIRED="DB_URL DB_USERNAME DB_PASSWORD REDIS_HOST JWT_SECRET \
MAIL_USERNAME MAIL_PASSWORD \
ALIYUN_OSS_ENDPOINT ALIYUN_OSS_BUCKET_NAME ALIYUN_OSS_ACCESS_KEY_ID ALIYUN_OSS_ACCESS_KEY_SECRET \
WX_MINI_APPID WX_MINI_SECRET \
TENCENT_SMS_SECRET_ID TENCENT_SMS_SECRET_KEY TENCENT_SMS_SDK_APP_ID TENCENT_SMS_SIGN_NAME \
TENCENT_SMS_LOGIN_TEMPLATE_ID TENCENT_SMS_RESET_PASSWORD_TEMPLATE_ID TENCENT_SMS_CHANGE_PHONE_TEMPLATE_ID \
TENCENT_MAP_KEY APP_BASE_URL CORS_ALLOWED_ORIGINS"
for k in $REQUIRED; do
  v="$(getval "$k")"
  if [ -z "$v" ]; then no "$k 未设置"
  elif is_placeholder "$v"; then wa "$k 仍是占位/示例值: $v"
  else ok "$k 已填"; fi
done

echo "[2] JWT_SECRET 强度"
j="$(getval JWT_SECRET)"
if [ -n "$j" ] && ! is_placeholder "$j"; then
  if [ "${#j}" -ge 32 ]; then ok "长度 ${#j} (>=32)"; else no "长度 ${#j} (<32，启动会失败)"; fi
fi

echo "[3] 微信支付（WX_PAY_ENABLED=true 时必需）"
wx="$(getval WX_PAY_ENABLED)"
if [ "$wx" = "true" ]; then
  for k in WX_PAY_APPID WX_PAY_MCH_ID WX_PAY_MERCHANT_SERIAL_NUMBER WX_PAY_PRIVATE_KEY_PATH WX_PAY_API_V3_KEY; do
    v="$(getval "$k")"
    if [ -n "$v" ] && ! is_placeholder "$v"; then ok "$k 已填"; else no "$k 缺失/占位"; fi
  done
  pk="$(getval WX_PAY_PRIVATE_KEY_PATH)"
  if [ -n "$pk" ]; then
    if [ -f "$pk" ]; then
      if head -1 "$pk" | grep -q 'BEGIN .*PRIVATE KEY'; then ok "私钥是 PEM: $pk"; else no "私钥头部异常(非 PEM): $pk"; fi
      p="$(stat -c '%a' "$pk" 2>/dev/null || echo '?')"
      if [ "$p" = "600" ]; then ok "私钥权限 600"; else wa "私钥权限 $p（建议 600）"; fi
    else no "私钥文件不存在: $pk"; fi
  fi
else
  wa "WX_PAY_ENABLED != true（支付未启用，跳过；联调真实支付时再开）"
fi

echo "[4] 本机服务连通性"
if command -v mysqladmin >/dev/null 2>&1; then
  if mysqladmin ping --silent >/dev/null 2>&1; then ok "MySQL ping"; else wa "MySQL ping 失败（root 可能走 auth_socket，非致命）"; fi
fi
if command -v redis-cli >/dev/null 2>&1; then
  rh="$(getval REDIS_HOST)"; rh="${rh:-127.0.0.1}"
  if [ "$(redis-cli -h "$rh" ping 2>/dev/null)" = "PONG" ]; then ok "Redis ping ($rh)"; else no "Redis ping 失败 ($rh)"; fi
fi
if command -v curl >/dev/null 2>&1; then
  body="$(curl -s --max-time 3 http://127.0.0.1:9090/actuator/health 2>/dev/null)"
  if [ -n "$body" ]; then ok "后端 health(9090): $body"
  else wa "后端 9090 无响应（未启动或未就绪）"; fi
fi

echo "[5] 对外回调/域名可达（需 nginx + 证书已就绪）"
for u in "$(getval WX_PAY_NOTIFY_URL)" "$(getval WX_PAY_REFUND_NOTIFY_URL)" "$(getval APP_BASE_URL)"; do
  [ -z "$u" ] && continue
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 6 -I "$u" 2>/dev/null)"
  case "$code" in
    200|204|301|302|403|404|405) ok "$u -> HTTP $code（可达）" ;;
    *) wa "$u -> ${code:-无响应}" ;;
  esac
done

echo
echo "==================== 汇总: OK=$pass  WARN=$warn  FAIL=$fail ===================="
if [ "$fail" -gt 0 ]; then echo "存在 FAIL 项，请先修复再联调。"; exit 1; fi
echo "无致命问题（WARN 多为占位值，按需补全）。"
