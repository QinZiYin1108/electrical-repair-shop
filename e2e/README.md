# 端到端测试（Playwright）

电气维修平台关键路径的端到端测试脚手架（`REMAINING_IMPLEMENTATION_PHASES.md` 第六阶段 4.4）。

## 覆盖范围

| 目标 | 说明 | 是否可在本仓库直接运行 |
| --- | --- | --- |
| `official-website` | 官网（Astro 静态站）首页烟测 | 是（自动 `npm run preview`） |
| `admin-front-end` | 后台（Vue3）登录页烟测 | 需先启动后台并设置 `E2E_ADMIN_URL` |
| 维修端（worker-front-end） | uni-app，可用 H5 构建后作为静态站接入（见下） | 可选 |
| 用户端（user-front-end） | 原生微信小程序，**无法用浏览器 Playwright 覆盖**，需微信开发者工具（自动化留待后续） | 否 |

## 快速开始

```bash
cd e2e
npm install
npm run install:browsers   # 安装 Chromium（含依赖）
npm test                   # 运行全部项目（官网自动起预览服务）
```

仅官网：

```bash
npm run test:official
```

后台（需另开终端先启动后台）：

```bash
# 终端 A
cd ../admin-front-end && npm install && npm run serve
# 终端 B
cd e2e
E2E_ADMIN_URL=http://localhost:8080 npm run test:admin
```

## 环境变量

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `E2E_OFFICIAL_URL` | `http://localhost:4321` | 官网地址；设置后不再自动启动 `webServer` |
| `E2E_ADMIN_URL` | `http://localhost:8080` | 后台地址；**未设置则跳过后台用例** |
| `CI` | - | 为真时启用重试、单 worker、不复用已有服务器 |

## CI 建议

```bash
cd e2e
npm ci
npx playwright install --with-deps chromium
# 先构建/启动被测前端，或设置 E2E_*_URL 指向已部署地址
npm test
```

## 说明与限制

- 本机（开发环境）通常无浏览器，`npm test` 会因缺少 Chromium 失败；请先 `npm run install:browsers`，或在 CI 中运行。
- 维修端若需 E2E：先在 `worker-front-end` 用 HBuilderX/uni-app 构建 H5，静态托管后用 `webServer`/`E2E_*_URL` 指向该地址即可复用同一套用例骨架。
- 当前用例为**烟测**（可达、渲染、基本元素），用于打通 E2E 链路；后续可在此基础上补齐下单/接单/支付/取消/售后/消息等关键路径。
