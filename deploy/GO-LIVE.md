# 从零上线详细步骤（Debian 13）

> 适用：Debian 13 (trixie) + Oracle MySQL 8.4 + OpenJDK 25 + Node 20 + Nginx + Let's Encrypt。
> 域名：`thdqwx.work`（根域→www）、`www.thdqwx.work`（官网）、`admin.thdqwx.work`（后台）、`api.thdqwx.work`（后端）。
> 变量约定：`<SRC>`=`/opt/electrical-repair-shop/src`，`<ROOT>`=`/opt/electrical-repair-shop`，`<SERVER>`=`root@服务器公网IP`。

每个步骤都给出：**目的 → 命令 → 预期 → 排错**。遇到报错先看「排错」，再把输出发我。

---

## 步骤 0：准备（本地 + DNS）

**目的**：确保能连上服务器、域名解析正确、端口放行。

1) 本地能 SSH 登录（Windows PowerShell）：
```powershell
ssh <SERVER>          # 首次会提示输入密码/接受指纹
```

2) 域名解析（在本地或服务器执行均可）：
```bash
dig +short thdqwx.work www.thdqwx.work admin.thdqwx.work api.thdqwx.work
```
**预期**：四条都返回你的服务器公网 IP。

3) 云安全组放行入站：`22`（SSH）、`80`、`443`。

4) 服务器已装 `git`（若要用 git 方式拉代码）。检查 `git --version`；没有则先 `apt-get update && apt-get install -y git`（或直接跑步骤 2 的 `server-bootstrap.sh`，它已包含 git）。

**排错**：
- `dig` 没返回 IP → DNS 未生效或未加记录，等到生效（可 `dig +trace`）。
- SSH 连不上 → 检查安全组 22、`ping` 公网 IP。

---

## 步骤 1：把源码放到服务器（仅首次）

**目的**：让服务器上有仓库（含 `deploy/` 脚本），后续才能用推送部署。

方式 A（服务器能访问 Git，**推荐 XShell 场景**）：
```bash
sudo mkdir -p <SRC>
sudo git clone -b feat/new-thing https://github.com/QinZiYin1108/electrical-repair-shop.git <SRC>
# 之后在 XShell 里一条命令更新并部署：bash <SRC>/deploy/server-pull-deploy.sh
```
> 私有仓库需先给服务器配置凭据：HTTPS 用 Personal Access Token（`git config --global credential.helper store` 后首次输入），或改用 SSH（配 Deploy Key）。

方式 A2（**服务器拉不动 GitHub 时——国内常见 `GnuTLS recv error (-110)`，改用 Gitee 镜像**）：
```bash
# 1) 在 Gitee 导入仓库：登录 gitee.com → 右上「+」→「新建仓库」→ 选「导入」
#    打开 https://gitee.com/projects/import/url ，填 GitHub 仓库地址，导入（可设公开/私有）
# 2) 服务器从 Gitee 克隆
sudo mkdir -p <SRC>
sudo git clone -b feat/new-thing https://gitee.com/<你的Gitee用户名>/electrical-repair-shop.git <SRC>
# 3) 之后更新（origin 即 Gitee）：bash <SRC>/deploy/server-pull-deploy.sh
```
> Gitee 镜像是**一次性快照**：GitHub 侧后续提交需在 Gitee 仓库页点「同步」/重新导入，或本地直接 push 到 Gitee。

方式 B（本地推送，本地仓库根目录执行）：
```powershell
tar -czf app-src.tar.gz --exclude node_modules --exclude target --exclude dist --exclude .git .
scp app-src.tar.gz <SERVER>:/tmp/
ssh <SERVER> "mkdir -p /opt/electrical-repair-shop/src && tar xzf /tmp/app-src.tar.gz -C /opt/electrical-repair-shop/src"
```

**验证**：
```bash
ls <SRC>/deploy          # 应看到 server-bootstrap.sh 等
ls <SRC>/main/sql/electrical_repair_shop_init.sql
```

**排错**：`scp`/`tar` 不存在 → Windows 10+ 自带；旧系统用 `Compress-Archive` 或用 WinSCP 手动上传。

---

## 步骤 2：一键安装服务器环境

**目的**：装好 JDK25 / Node20 / MySQL8.4 / Redis / Nginx / certbot，并创建 swap、目录、systemd 服务。

```bash
sudo bash <SRC>/deploy/server-bootstrap.sh
```

**预期**：脚本逐段输出，最后打印「首次准备完成」清单。

**逐项验证**：
```bash
java -version            # openjdk version "25"
node -v && npm -v        # v20.x
mysqld --version         # 8.4.x
nginx -v                 # 1.26.x
redis-cli ping           # PONG
free -h                  # 看到 Swap: 2.0Gi
systemctl is-enabled electrical-backend   # 显示服务已安装（可能尚未 enable）
```

**排错**：
- `openjdk-25-jdk` 报无包 → 脚本会自动回退 Adoptium；若仍失败，手动 `apt-get install temurin-25-jdk`。
- MySQL 源不可达（`repo.mysql.com`）→ 换官方 `mysql-apt-config` deb：`dpkg -i mysql-apt-config_*.deb` 选 `mysql-8.4-lts` → `apt update && apt install mysql-community-server`。
- `swapfile` 已存在报错 → 可忽略，或 `swapoff` 后重跑。

---

## 步骤 3：配置后端环境变量 `.env`

**目的**：后端以 `prod` profile 运行，全部配置来自该文件。

```bash
sudo cp <SRC>/deploy/env/backend.env.example <ROOT>/.env
sudo vi <ROOT>/.env
```

**必填/确认项**：
- `DB_URL=jdbc:mysql://127.0.0.1:3306/electrical_repair_shop?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8`
- `DB_USERNAME=app`、`DB_PASSWORD=<步骤4设的密码>`
- `REDIS_HOST=127.0.0.1`、`REDIS_PASSWORD=`（本机无密码留空）
- `JWT_SECRET=<长随机串，至少32位>`
- `CORS_ALLOWED_ORIGINS=https://admin.thdqwx.work,https://www.thdqwx.work`
- `APP_BASE_URL=https://api.thdqwx.work`
- `MAIL_*`、`ALIYUN_OSS_*`、`WX_MINI_*`、`TENCENT_SMS_*`、`TENCENT_MAP_KEY` 按实填
- `WX_PAY_ENABLED=false`（联调再开）
- `ADMIN_API_BASE_URL=https://api.thdqwx.work/api`
- `FLYWAY_ENABLED=false`（生产再定）

**排错**：某字段留空会导致启动报错——启动失败时先核对 `.env` 是否有漏项（见步骤 6）。

---

## 步骤 4：初始化数据库

**目的**：建库、建应用用户、导入 69 张表 + 基础配置。

```bash
# 建库
sudo mysql -e "CREATE DATABASE IF NOT EXISTS electrical_repair_shop DEFAULT CHARACTER SET utf8mb4;"
# 建应用用户（应用用这个，不用 root）
sudo mysql -e "CREATE USER IF NOT EXISTS 'app'@'localhost' IDENTIFIED BY '你的强密码'; GRANT ALL PRIVILEGES ON electrical_repair_shop.* TO 'app'@'localhost'; FLUSH PRIVILEGES;"
# 导入初始化脚本
sudo mysql electrical_repair_shop < <SRC>/main/sql/electrical_repair_shop_init.sql
```

**验证**：
```bash
sudo mysql electrical_repair_shop -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='electrical_repair_shop';"   # 期望 69
sudo mysql electrical_repair_shop -e "SELECT username FROM admin_accounts;"       # 含 admin
sudo mysql electrical_repair_shop -e "SELECT config_key FROM system_configs LIMIT 5;"
```

**预期**：默认管理员 `admin` / `admin123456`。

**排错**：
- `Access denied for user 'app'` → 密码与 `.env` 不一致，或 GRANT 未执行。
- 导入报语法错 → 确认用的是 `electrical_repair_shop_init.sql`（MySQL 语法）；若误用其它 dump 请更换。

---

## 步骤 5：（可选，第四阶段）上传微信支付证书

**目的**：真实支付需要商户私钥。

```bash
sudo mkdir -p <ROOT>/certs
sudo cp apiclient_key.pem <ROOT>/certs/apiclient_key.pem
sudo chmod 600 <ROOT>/certs/apiclient_key.pem
sudo chown root:root <ROOT>/certs/apiclient_key.pem
```
`.env` 中 `WX_PAY_PRIVATE_KEY_PATH=<ROOT>/certs/apiclient_key.pem`（步骤 3 已含占位）。

**排错**：私钥读不到 → 权限 600 + 属主正确；路径写绝对路径。

---

## 步骤 6：启动后端并验证

**目的**：让 Spring Boot 起来，确认能连 DB/Redis。

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now electrical-backend
sudo systemctl status electrical-backend --no-pager
```

**验证（健康检查，管理端口 9090）**：
```bash
curl -s http://127.0.0.1:9090/actuator/health        # 期望 {"status":"UP"}
```

**看日志**：
```bash
journalctl -u electrical-backend -n 80 --no-pager
```

**排错**（最常见）：
- 服务起不来 → `journalctl` 里通常是 `.env` 缺项、DB 连不上、Redis 连不上。
- `Access denied` → `.env` 的 `DB_USERNAME/DB_PASSWORD` 与步骤 4 不一致。
- `Communications link failure` → MySQL 未启动：`systemctl status mysql`。
- 端口占用 → `ss -lntp | grep 8081`。

---

## 步骤 7：部署 Nginx 站点 + HTTPS 证书

**目的**：4 个域名走 HTTPS，官网/后台静态、api 反代。

```bash
EMAIL=you@example.com sudo -E bash <SRC>/deploy/setup-nginx-ssl.sh
```

**预期**：certbot 成功签发（证书在 `/etc/letsencrypt/live/thdqwx.work/`），脚本部署配置并 reload。

**验证**：
```bash
sudo nginx -t
sudo certbot certificates | grep -A2 thdqwx.work
curl -I https://www.thdqwx.work
curl -I https://admin.thdqwx.work
curl -s https://api.thdqwx.work/api/actuator/health    # 需步骤6已完成
```

**排错**：
- certbot 失败 `Could not verify` → DNS 未解析或 80 未放行；确认 `dig +short` 与安全组。
- `nginx: [emerg] unknown directive "http2"` → 版本 <1.25（不太可能，Debian13 是 1.26）；本仓库配置已用 `http2 on;`。
- 端口 80 被占用 → 先停其它占 80 的服务。

---

## 步骤 8：首次构建部署（本地执行）

**目的**：服务器上构建后端 jar 与前端 dist，发布并重启。

```powershell
cd <本地仓库根目录>
./deploy/push-deploy.ps1 -Server <SERVER>
```

**脚本行为**：本地 `tar`（排除 node_modules/target/dist/.git）→ `scp` 到 `/tmp/app-src.tar.gz` → `ssh` 执行 `remote-deploy.sh`（解包→`build-deploy.sh`：后端 `mvnw package`、官网/后台 `npm ci && build`、发布→`systemctl restart`）。

**预期**：末尾出现远端「部署完成」，且 `systemctl restart electrical-backend` 成功。

**验证**：浏览器
- `https://www.thdqwx.work` 打开官网
- `https://admin.thdqwx.work` 打开后台，用 `admin/admin123456` 登录，**随后在个人中心修改密码**

**排错**：
- `Permission denied (publickey)` → ssh 密钥/`-SshKey` 参数。
- `mvnw: Permission denied` → 远端脚本已 `chmod +x`；若手跑需 `chmod +x <SRC>/back-end/mvnw`。
- `\r` 报错（CRLF）→ `sed -i 's/\r$//' <SRC>/back-end/mvnw <SRC>/deploy/*.sh`。
- 构建 OOM → 已含 2G swap + 限堆；用 `-SkipBackend`/`-SkipFrontend` 分开构建，或错峰。

---

## 步骤 9：端到端验收

- [ ] `https://www.thdqwx.work` 正常
- [ ] `https://admin.thdqwx.work` 可打开、`admin/admin123456` 可登录（登录后改密码）
- [ ] `https://api.thdqwx.work/api/actuator/health` = `{"status":"UP"}`
- [ ] `curl -I http://thdqwx.work` 返回 301 → `https://www.thdqwx.work`
- [ ] 数据库 69 张表、`system_configs` 有数据
- [ ] 三端接口基址指向 `https://api.thdqwx.work`

---

## 步骤 10：日常更新流程

**方式一（XShell，服务器端拉取，推荐）**：
```bash
bash <SRC>/deploy/server-pull-deploy.sh              # git 拉取 feat/new-thing 并构建部署
bash <SRC>/deploy/server-pull-deploy.sh --skip-frontend
```
**方式二（本地 PowerShell 推送）**：
```powershell
# 全量（后端+官网+后台）
./deploy/push-deploy.ps1 -Server <SERVER>
# 仅后端
./deploy/push-deploy.ps1 -Server <SERVER> -SkipFrontend
# 仅前端
./deploy/push-deploy.ps1 -Server <SERVER> -SkipBackend
# 指定后台构建基址
./deploy/push-deploy.ps1 -Server <SERVER> -AdminApiBaseUrl https://api.thdqwx.work/api
```

---

## 步骤 11：第四阶段真实支付联调（资金相关，先备份）

1) 备份数据库：
```bash
sudo mysqldump --single-transaction --routines --triggers electrical_repair_shop > ~/backup_$(date +%F_%H%M).sql
```
2) `.env` 填写：`WX_PAY_ENABLED=true`、`WX_PAY_APPID`、`WX_PAY_MCH_ID`、`WX_PAY_MERCHANT_SERIAL_NUMBER`、`WX_PAY_PRIVATE_KEY_PATH`、`WX_PAY_API_V3_KEY`、`WX_PAY_NOTIFY_URL=https://api.thdqwx.work/api/pass/payments/wechat/notify`、`WX_PAY_REFUND_NOTIFY_URL=.../refund-notify`
3) 重启：`sudo systemctl restart electrical-backend`
4) 微信商户平台配置回调域名白名单（`api.thdqwx.work`）
5) 小程序端用最小金额下单一笔，验证：预下单 → `wx.requestPayment` → 回调入账 → 订单状态推进；再验证退款闭环。
6) 观察：`journalctl -u electrical-backend -f`、`curl -s https://api.thdqwx.work/api/actuator/health`

**注意**：`WX_PAY_ENABLED=true` 后表示真金白银，任何改动前先备份。

---

## 步骤 12：安全加固与备份（建议尽早）

```bash
# 防火墙（Debian 13 若用 ufw）
sudo apt-get install -y ufw
sudo ufw allow 22/tcp && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp
sudo ufw enable

# SSH 改密钥登录、关密码登录（编辑 /etc/ssh/sshd_config: PasswordAuthentication no; 重启 ssh）

# 每天 3 点备份数据库，保留 14 天
cat | sudo tee /etc/cron.d/db-backup >/dev/null <<'EOF'
0 3 * * * root mysqldump --single-transaction electrical_repair_shop > /root/backup_$(date +\%F).sql && find /root -name 'backup_*.sql' -mtime +14 -delete
EOF
```

---

## 步骤 13：故障排查总表

| 现象 | 排查 |
| --- | --- |
| 后端启动失败 | `journalctl -u electrical-backend -n 100 --no-pager`；多为 `.env` 缺项/DB/Redis 连接 |
| DB 拒绝连接 | `systemctl status mysql`；`app` 用户与密码、GRANT；端口 3306 |
| 503/502（api 域） | 后端是否在 8081：`ss -lntp \| grep 8081` |
| 站点 404/空白 | 是否已首次部署；`ls <ROOT>/official-website` 是否为空 |
| certbot 失败 | DNS/80；`dig +short`；`sudo certbot certificates` |
| `mvnw`/CRLF | `chmod +x`；`sed -i 's/\r$//' ...` |
| 构建 OOM | `dmesg -T \| grep -i oom`；用 `-Skip*`、错峰、或升 4C8G |
| 证书到期 | 自动续期；`sudo certbot renew --dry-run` 测试 |
| MySQL 源 key 过期（`not live ... Expired`/`EXPKEYSIG B7B3B788A8D3785C`） | 2025-10 旧签名 key 过期；用官方 `mysql-apt-config`（含刷新 key）或从 keyserver 重取 `B7B3B788A8D3785C`；脚本已自动处理 |

---

## 附录：关键路径 / 端口 / 命令

| 项 | 值 |
| --- | --- |
| 源码 | `<SRC>` |
| 后端 jar | `<ROOT>/backend/app.jar` |
| 官网站点 | `<ROOT>/official-website` |
| 后台站点 | `<ROOT>/admin-front-end` |
| 后端 env | `<ROOT>/.env` |
| systemd | `electrical-backend` |
| 端口 | 后端 8081（ctx `/api`）、管理 9090（127.0.0.1）、MySQL 3306、Redis 6379、Nginx 80/443 |
| 构建部署 | `bash <SRC>/deploy/build-deploy.sh [--skip-backend\|--skip-frontend]` |
| 日志 | `journalctl -u electrical-backend -f` |
