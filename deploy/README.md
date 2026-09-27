# 服务器自动构建与部署

目标：把源码从本地推到腾讯云服务器，在**服务器上**用 Maven + Node 自动构建、替换产物并重启服务，免去每次本地打 jar/dist 再上传的麻烦。

## 工作方式

```
本地(Windows)                         服务器(Linux)
-----------                          -------------
push-deploy.ps1
  ├─ tar 打包源码(排除 node_modules/target/dist/.git)
  ├─ scp 上传到 /tmp/app-src.tar.gz
  └─ ssh 执行 remote-deploy.sh  ───▶  remote-deploy.sh
                                        ├─ 解包覆盖源码目录
                                        └─ build-deploy.sh
                                             ├─ back-end: ./mvnw clean package → backend/app.jar
                                             ├─ official-website: npm ci && npm run build → 发布 dist
                                             ├─ admin-front-end: npm ci && VUE_APP_API_BASE_URL=... npm run build → 发布 dist
                                             └─ systemctl restart electrical-backend
```

每次改完代码，只需在本地运行一次 `deploy/push-deploy.ps1`。

## 目录约定（服务器）

| 路径 | 用途 |
| --- | --- |
| `/opt/electrical-repair-shop/src` | 源码（本地推送解包到此） |
| `/opt/electrical-repair-shop/backend/app.jar` | 后端运行 jar |
| `/opt/electrical-repair-shop/official-website` | 官网静态站发布目录（Nginx root） |
| `/opt/electrical-repair-shop/admin-front-end` | 后台静态站发布目录（Nginx root） |
| `/opt/electrical-repair-shop/.env` | 后端环境变量（systemd EnvironmentFile） |

> 与既有的 Nginx 站点一致：`www` → `official-website`，`admin` → `admin-front-end`，`api` → `http://127.0.0.1:8081/api`。

## 从零搭建（服务器重置后）

目标：**Debian 13 (trixie)** 全新服务器。域名 4 个：`thdqwx.work`（根域，301→www）、`www.thdqwx.work`（官网）、`admin.thdqwx.work`（后台）、`api.thdqwx.work`（后端）。HTTPS 用 Let's Encrypt。

> Debian 13 要点：主源自带 **OpenJDK 25** 与 **Node 20**；数据库使用 **Oracle MySQL 8.4 LTS**（官方 APT 源，组件 `mysql-8.4-lts`，客户端命令 `mysql`）；nginx 1.26 采用 `http2 on;` 新语法（本仓库配置已适配）。

```bash
# 0) DNS：把 thdqwx.work / www / admin / api 四条 A 记录都指向服务器公网 IP，等待生效

# 1) 放置源码（首次）：把本仓库放到 /opt/electrical-repair-shop/src
sudo mkdir -p /opt/electrical-repair-shop/src
#   方式 A（服务器能访问 Git，推荐）：git clone -b feat/new-thing https://github.com/QinZiYin1108/electrical-repair-shop.git /opt/electrical-repair-shop/src
#   方式 B（本地推送）：见 deploy/push-deploy.ps1
#   走方式 A 时，日后在服务器一条命令更新：bash /opt/electrical-repair-shop/src/deploy/server-pull-deploy.sh

# 2) 一键准备基础环境（JDK25 / Node20 / MySQL / Redis / Nginx / certbot + 2G swap + systemd）
sudo bash /opt/electrical-repair-shop/src/deploy/server-bootstrap.sh

# 3) 配置后端环境变量
sudo cp /opt/electrical-repair-shop/src/deploy/env/backend.env.example /opt/electrical-repair-shop/.env
sudo vi /opt/electrical-repair-shop/.env   # 填数据库/Redis/JWT/OSS/SMS/微信支付等

# 4) 建库并导入初始化脚本（全部表结构 + 基础配置；不含业务样例数据）
#    Oracle MySQL 8.4：root 走 auth_socket，用 sudo mysql
sudo mysql -e "CREATE DATABASE IF NOT EXISTS electrical_repair_shop DEFAULT CHARACTER SET utf8mb4;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'app'@'localhost' IDENTIFIED BY 'your-db-password'; GRANT ALL PRIVILEGES ON electrical_repair_shop.* TO 'app'@'localhost'; FLUSH PRIVILEGES;"
sudo mysql electrical_repair_shop < /opt/electrical-repair-shop/src/main/sql/electrical_repair_shop_init.sql
#    .env 中 DB_USERNAME=app、DB_PASSWORD=your-db-password
#    默认管理员：admin / admin123456（登录后请立即修改）

# 5) 启动后端
sudo systemctl daemon-reload
sudo systemctl enable --now electrical-backend
sudo systemctl status electrical-backend

# 6) 部署 Nginx 站点 + Let's Encrypt 证书（域名需已解析）
EMAIL=you@example.com sudo -E bash /opt/electrical-repair-shop/src/deploy/setup-nginx-ssl.sh

# 7) 本地首次部署（构建后端/官网/后台并重启）
./deploy/push-deploy.ps1 -Server root@IP
```

站点模板：`deploy/nginx/thdqwx.work.conf`（www=官网 / admin=后台 / api=后端 / 根域 301→www），
证书路径 `/etc/letsencrypt/live/thdqwx.work/`，续期由 certbot 定时器自动完成。

## 每次部署（本地）

```powershell
# 在仓库根目录执行
./deploy/push-deploy.ps1 -Server root@1.2.3.4
```

可选参数：

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `-Server` | 必填 | `user@host` |
| `-SshKey` | 空 | 私钥路径（`-i`），留空用默认/`ssh-agent` |
| `-SkipBackend` | `false` | 跳过后端构建 |
| `-SkipFrontend` | `false` | 跳过前端构建 |
| `-AdminApiBaseUrl` | `.env` 中 `ADMIN_API_BASE_URL` 或 `https://api.thdqwx.work/api` | 后台构建期 API 基址 |

脚本会：本地 `tar` 打包 → `scp` 上传 → `ssh` 触发服务器构建与重启。

## 环境变量（`.env`）

后端生产 profile 为 `prod`（`application-prod.properties`），全部配置由环境变量注入。示例见
`deploy/env/backend.env.example`，关键项：

- `SPRING_PROFILES_ACTIVE=prod`
- 数据库：`DB_URL` / `DB_USERNAME` / `DB_PASSWORD`
- Redis：`REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`
- 安全：`JWT_SECRET`、`CORS_ALLOWED_ORIGINS`（例如 `https://admin.thdqwx.work,https://www.thdqwx.work`）
- 支付（联调真实商户时）：`WX_PAY_ENABLED=true` 及 `WX_PAY_*`（商户号、证书序列号、`apiclient_key.pem` 绝对路径、APIv3 密钥、回调地址）
- 短信/地图/OSS/小程序：见示例
- 数据库迁移：`FLYWAY_ENABLED=true`（**启用前先备份**；已有库由 baseline 建立基线，不重跑 V1）
- 后台构建基址：`ADMIN_API_BASE_URL`（构建期用）

## 小内存服务器（2 核 4GB）注意

2 核 4GB 同时运行 JDK 后端 + MySQL + Redis + 其它站点时，**运行期已偏紧，构建期最吃内存**。可承受，但需约束：

- **加 swap**：`server-bootstrap.sh` 会自动创建 2G swapfile；若无请手动加（防构建 OOM 直接被杀）。
- **串行构建**：`build-deploy.sh` 已按「后端 → 官网 → 后台」串行，切勿并行。
- **限制堆**：脚本已默认 `MAVEN_OPTS=-Xmx1024m`、`NODE_OPTIONS=--max-old-space-size=1024`；若构建 OOM 可调小（512m），若编译内存不足再调大。
- **降载**：构建窗口尽量错峰；可用 `PAUSE_SERVICES` 让脚本在**构建期间自动暂停**其它服务（如个人博客）并在构建结束后自动恢复：
  ```bash
  PAUSE_SERVICES="blog.service" bash deploy/build-deploy.sh
  ```
  这样无需永久下线博客；也可先 `systemctl stop 个人博客`，构建完再起。
- **只构建变更端**：`push-deploy.ps1 -SkipBackend` 或 `-SkipFrontend`，避免整轮全量构建。
- **MySQL 调小**：`innodb_buffer_pool_size` 设 128–256M；Redis 设 `maxmemory 128mb`，避免与构建争内存。
- **排查 OOM**：`dmesg -T | grep -i oom`、`journalctl -k | grep -i oom`、`free -h`。
- 若频繁吃紧：升级到 4 核 8GB，或改为「本地/CI 构建后只推产物」。

## 说明与注意

- **Debian 13 组件来源**：`OpenJDK 25` 与 `Node 20` 直接来自主源（`apt install openjdk-25-jdk nodejs npm`）；脚本已优先用主源，缺失才回退 Adoptium / NodeSource。
- **数据库**：使用 **Oracle MySQL 8.4 LTS**（官方 APT 源，组件 `mysql-8.4-lts`，Codename 取系统 `trixie`）；客户端命令为 `mysql`（非 `mariadb`），`root@localhost` 默认 `auth_socket`（用 `sudo mysql` 连接），应用建议用独立用户 `app`（见第 4 步）。
- **JDK 25**：`back-end/pom.xml` 指定 `<java.version>25</java.version>`。
- **无需单独装 Maven**：仓库自带 Maven Wrapper（`back-end/mvnw`），首次构建会自动下载。
- **nginx**：Debian 13 为 1.26，配置使用 `http2 on;`（`deploy/nginx/thdqwx.work.conf` 已适配）。
- **如需改用 MariaDB（可选）**：Debian 13 主源默认 MariaDB 11.x（`apt install mariadb-server`，客户端 `mariadb`）。项目用标准 SQL，二者皆可；本部署以 Oracle MySQL 8.4 为准。
- **换行符**：`.gitattributes` 已设 `*.sh` 为 LF；若从 Windows 直接复制导致 CRLF，脚本会报 `\r` 错误，可 `sed -i 's/\r$//' deploy/*.sh`。
- **首次构建较慢**：`npm ci` 与 Maven 下载依赖耗时较长属正常。
- **回滚**：可在 `build-deploy.sh` 前先备份 `backend/app.jar` 与两个静态目录（见脚本内可选备份段）。
- **维修端小程序 / uni-app**：构建需 HBuilderX，不在本流程内，单独发布。
