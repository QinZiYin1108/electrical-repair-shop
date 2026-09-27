# 开发与检查命令

本文档用于固化本项目的本地开发、环境配置和提交前检查命令。

## 1. 环境配置

### 后端

后端支持两种配置方式：

- 本地开发：复制 `back-end/src/main/resources/application-local.properties.example` 为 `back-end/src/main/resources/application-local.properties`。
- 环境变量方式：复制根目录 `.env.example` 为 `.env`，按实际环境填写。

关键要求：

- `security.jwt.secret` / `JWT_SECRET` 必须是至少 32 位的非默认随机字符串。
- `prod` 环境会强校验数据库、Redis、邮件、OSS、内容审核、微信小程序、腾讯短信、腾讯地图、应用访问地址等配置。
- 微信支付默认关闭。只有准备真实商户联调时才设置 `WX_PAY_ENABLED=true`，并同时配置 `WX_PAY_APPID`、`WX_PAY_MCH_ID`、`WX_PAY_MERCHANT_SERIAL_NUMBER`、`WX_PAY_PRIVATE_KEY_PATH`、`WX_PAY_API_V3_KEY` 和 HTTPS `WX_PAY_NOTIFY_URL`。
- 不得把商户私钥、API v3 密钥或证书文件提交到仓库；启用微信支付后，缺少或格式错误的配置会阻止后端启动。
- 本地 `local` 环境只强校验 JWT 密钥，外部服务缺失会在调用对应功能时暴露。

### 管理端

复制示例配置：

```bash
cp admin-front-end/.env.development.example admin-front-end/.env.development
cp admin-front-end/.env.production.example admin-front-end/.env.production
```

本地开发默认通过 `/api` 代理到 `VUE_APP_DEV_PROXY_TARGET`。

### 用户小程序

默认 API 地址在 `user-front-end/api/config.js` 中是本地后端：

```text
http://localhost:8081/api
```

如需覆盖，复制：

```bash
cp user-front-end/api/config.local.example.js user-front-end/api/config.local.js
```

`config.local.js` 已加入 `.gitignore`。

### 师傅端

默认 API 地址在 `worker-front-end/api/config.js` 中是本地后端：

```text
http://localhost:8081/api
```

如需覆盖，复制：

```bash
cp worker-front-end/api/config.local.example.js worker-front-end/api/config.local.js
```

`config.local.js` 已加入 `.gitignore`。

## 2. 安装依赖

后端使用 Maven Wrapper，无需额外安装 Maven。

前端依赖分别安装：

```bash
cd admin-front-end
npm install

cd ../official-website
npm install

cd ../user-front-end
npm install

cd ../worker-front-end
npm install
```

## 3. 启动命令

### 后端

```bash
cd back-end
.\mvnw.cmd spring-boot:run
```

默认访问地址：

```text
http://localhost:8081/api
```

### 管理端

```bash
cd admin-front-end
npm run serve
```

默认访问地址：

```text
http://localhost:8080
```

### 官网

```bash
cd official-website
npm run dev
```

用户小程序和师傅端使用微信开发者工具 / HBuilderX 打开对应目录运行。

## 4. 提交前检查

在仓库根目录运行全部检查：

```powershell
.\scripts\check-all.ps1
```

如果本机 PowerShell 禁止执行脚本，可显式使用当前进程级绕过方式：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-all.ps1
```

### 后端

```bash
cd back-end
.\mvnw.cmd spotless:check
.\mvnw.cmd -DskipTests compile
```

后端测试是提交前必跑项：

```bash
.\mvnw.cmd test
```

### 管理端

```bash
cd admin-front-end
npm run format:check
npm run lint
npm run build
```

或：

```bash
npm run check
```

### 官网

```bash
cd official-website
npm run format:check
npm run build
```

或：

```bash
npm run check
```

### 用户小程序

```bash
cd user-front-end
npm run check
```

该命令检查格式、全部业务 JS 和 JSON 语法，并确认 `app.json` 中 44 个页面的 JS、JSON 和 WXML 文件存在。

### 师傅端

```bash
cd worker-front-end
npm run check
```

该命令检查格式、JS/JSON 语法、`pages.json` 页面清单，并使用 Vue SFC 编译器解析 25 个页面的脚本、模板和样式。HBuilderX 真机或目标平台构建仍属于发布前验收。

## 5. 格式化命令

### 后端

```bash
cd back-end
.\mvnw.cmd spotless:apply
```

### 前端

```bash
cd admin-front-end
npm run format

cd ../official-website
npm run format

cd ../user-front-end
npm run format

cd ../worker-front-end
npm run format
```
