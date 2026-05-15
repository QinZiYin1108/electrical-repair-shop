# 线上电器维修预约系统

基于 Spring Boot + Vue + 微信小程序的多角色电器维修预约与商城一体化平台。

---

## 一、系统简介

本系统是一套完整的 O2O 电器维修服务平台，覆盖用户下单、师傅接单、上门服务、商城购物、售后处理等全业务流程。系统包含三个端：

- **用户端（微信小程序）**：面向普通用户，提供维修预约下单、商品购买、订单跟踪、售后申请等功能
- **维修师傅端（Uni-app）**：面向维修师傅，提供接单、上门服务、收入查看、技能管理等功能
- **管理后台（Vue3）**：面向平台运营方，提供用户管理、师傅管理、订单管理、商品管理、售后处理、系统配置等功能

### 核心业务

#### 1. 维修预约服务
用户在线选择家电类别 → 描述故障现象 → 选择维修师傅 → 预约上门时间 → 支付费用 → 师傅上门维修 → 用户确认完成 → 评价。

支持三种服务模式：
- **上门维修**：师傅到用户指定地址维修
- **上门安装**：师傅上门安装电器设备
- **到店维修**：用户携带电器到店维修

#### 2. 商品商城
用户可在线购买电器商品（全新/二手），支持购物车、下单、收货、确认完成、申请售后、保修卡等完整电商流程。

#### 3. 售后处理
维修订单和商品订单均支持售后申请，包括退款、退货、换货、维修等类型，管理员审核处理后完成。

### 功能亮点

| 模块 | 功能 |
|------|------|
| **账号体系** | 多角色（用户/师傅/管理员）注册登录，邮箱验证码，微信一键登录，密码重置 |
| **维修下单** | 四级分类选品（家电类型→服务类型→故障现象→师傅选择），智能费用预估，时间段预约 |
| **师傅匹配** | 按技能、服务区域、工作时间筛选师傅，支持师傅关注 |
| **上门二维码** | 师傅上门时扫码核销，记录到达时间，防作弊 |
| **资金系统** | 三账户余额体系（用户/师傅/平台），充值、支付、退款、提现全链路追踪 |
| **优惠券** | 满减券、折扣券、免运费券，支持按商品/服务类别限定使用范围 |
| **消息推送** | 用户与师傅实时沟通，系统通知（订单状态、售后进度、账户变动） |
| **评价体系** | 用户对师傅和商品评价打分，管理员审核评价内容，师傅回复 |
| **保修卡** | 商品保修卡生成、核销、到期自动失效 |
| **操作日志** | 管理员所有关键操作留痕，可追溯 |
| **账号注销** | 用户和师傅均支持注销申请，带反悔期保护 |
| **系统配置** | 售后有效期、验证码过期时间、预约天数范围等参数动态配置 |

---

## 二、技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **后端框架** | Spring Boot 3 | Java Web 服务 |
| **ORM** | MyBatis-Plus | 数据库访问与分页 |
| **数据库** | MySQL 9.6 | 关系型数据存储 |
| **缓存** | Redis | 验证码、Session 管理 |
| **管理端** | Vue 3 + Element Plus | 管理员后台界面 |
| **用户端** | 微信小程序（Uni-app） | C 端用户下单入口 |
| **师傅端** | Uni-app + uView Plus | 师傅接单与工作端 |
| **对象存储** | 阿里云 OSS | 图片/视频/文件存储 |
| **地图服务** | 百度地图 API | 地址解析与地理位置 |
| **邮件服务** | QQ SMTP | 验证码与通知邮件 |

---

## 三、项目结构

本项目采用 **多分支管理**，各模块代码位于不同分支：

| 分支 | 内容 |
|------|------|
| `main` | 项目文档、数据库 SQL 脚本 |
| `backend` | Spring Boot 后端服务（Java） |
| `admin-front-end` | 管理后台前端（Vue3） |
| `user-front-end` | 用户端微信小程序（Uni-app） |
| `worker-front-end` | 维修师傅端（Uni-app） |

---

## 四、快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Node.js 16+（前端）
- 微信开发者工具（用户端调试）

### 1. 克隆仓库

```bash
git clone https://github.com/QinZiYin1108/electrical-repair-shop.git
cd electrical-repair-shop
```

### 2. 初始化数据库

```bash
mysql -u root -p < sql/electrical_repair_shop.sql
```

### 3. 启动后端

```bash
git checkout backend
cd back-end
# 配置环境变量（见下方配置说明）
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080/api`

### 4. 启动管理后台

```bash
git checkout admin-front-end
cd admin-front-end
npm install
npm run serve
```

管理后台默认运行在 `http://localhost:8081`

### 5. 启动用户端/师傅端

使用微信开发者工具或 HBuilderX 导入 `user-front-end` / `worker-front-end` 目录运行。

---

## 五、配置说明

后端敏感配置均通过**环境变量**注入，部署前需设置以下变量：

| 环境变量 | 说明 |
|----------|------|
| `DB_URL` | 数据库连接地址 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥（随机字符串） |
| `MAIL_USERNAME` | 邮箱账号（用于发送验证码） |
| `MAIL_PASSWORD` | 邮箱 SMTP 授权码 |
| `ALIYUN_OSS_ENDPOINT` | 阿里云 OSS Endpoint（如 oss-cn-beijing.aliyuncs.com） |
| `ALIYUN_OSS_BUCKET_NAME` | 阿里云 OSS Bucket 名称 |
| `ALIYUN_OSS_ACCESS_KEY_ID` | 阿里云 OSS AK |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | 阿里云 OSS SK |
| `WX_MINI_APPID` | 微信小程序 AppID |
| `WX_MINI_SECRET` | 微信小程序 AppSecret |
| `BAIDU_MAP_AK` | 百度地图 API Key |

> 注意：数据库初始化脚本 `sql/electrical_repair_shop.sql` 已包含 `system_configs` 表的 14 条系统配置初始化数据，其余业务表仅建表无数据。

---

## 六、数据库表概览

共 45 张表，按业务模块划分：

| 模块 | 表名 |
|------|------|
| **账号** | `user_accounts`, `technician_accounts`, `admin_accounts`, `user_profiles`, `technician_profiles`, `admin_profiles` |
| **订单** | `repair_orders`, `repair_order_faults`, `repair_order_payments`, `order_items`, `order_progress`, `order_door_qr_codes` |
| **商品** | `products`, `product_categories`, `product_orders`, `product_favorites`, `shopping_carts`, `warranty_cards`, `warranty_card_usage_records` |
| **售后** | `after_sales_applications` |
| **资金** | `account_balances`, `fund_flows`, `payment_records` |
| **评价** | `reviews` |
| **优惠券** | `coupons`, `user_coupons` |
| **消息** | `conversation_sessions`, `conversation_messages`, `system_messages` |
| **师傅** | `technician_skills`, `technician_service_areas`, `technician_work_times`, `technician_visit_fee_policies`, `user_follow_technicians` |
| **系统** | `system_configs`, `announcements`, `operation_logs`, `files`, `images`, `videos` |
| **地址** | `user_addresses` |
| **注销** | `account_cancel_records` |
| **服务** | `service_categories`, `service_types`, `fault_phenomena` |
