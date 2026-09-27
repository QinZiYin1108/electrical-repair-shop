# 后续优化建议

本文档记录当前项目建议继续推进的工程优化项。优先级按风险和收益排序，建议分阶段处理，避免一次性大改影响业务稳定性。

## P0：稳定性和基础工程

### 1. 建立完整构建检查

> 完成状态：已于 2026-07-12 实施。根目录新增 `scripts/check-all.ps1`；后端、管理端、官网、用户小程序和师傅端均有可执行检查。用户端覆盖 JS/JSON 与页面清单，师傅端增加 Vue SFC 编译验证。

目标：

- 后端、管理端、官网、用户端、师傅端都有明确的本地验证命令。
- 每次改动后能快速确认是否破坏编译、格式、基础功能。

建议：

- 后端保留并固化：
  - `mvnw.cmd spotless:check`
  - `mvnw.cmd -DskipTests compile`
  - 后续补齐测试后增加 `mvnw.cmd test`
- 管理端补充统一格式化工具，例如 Prettier，并增加 `format`、`format:check` 脚本。
- 官网 Astro 项目补充格式化和构建检查。
- 小程序/uni-app 端至少统一 ESLint/Prettier 或最小格式规则。

验证：

- 新增或更新 README 中的本地启动、构建、格式化命令。
- 在提交前能独立跑通后端编译和前端构建。

### 2. 清理和保护环境配置

> 完成状态：基础环境切换和启动校验已实施。微信支付配置使用独立开关且默认关闭；启用时校验商户号、API v3 密钥、商户私钥、证书序列号和 HTTPS 回调地址。真实商户密钥及渠道验收不纳入代码仓库。

目标：

- 避免接口地址、密钥、生产域名散落在代码里。
- 本地、测试、生产环境可切换。

建议：

- 用户端 `user-front-end/api/config.js` 不再硬编码 `http://localhost:8081/api`。
- 师傅端 `worker-front-end/api/config.js` 不再硬编码生产 API 地址。
- 管理端继续使用环境变量和代理配置，但补充 `.env.development.example`、`.env.production.example`。
- 后端启动时校验关键配置：
  - JWT secret 不能为默认值。
  - OSS、短信、微信、地图等外部服务缺失时给出清晰错误或降级策略。

验证：

- 本地环境只需复制 example 配置即可启动。
- 生产域名切换不需要改源码。

## P1：核心业务风险

> 完成状态：已于 2026-07-11 实施。订单状态保持现有 1-8 数字编码，数据库升级需先执行
> `main/sql/v0.3.0_migration_p1_consistency_security.sql`。

### 3. 抽象维修订单状态机

实施结果：已新增订单、支付、售后状态枚举与 `RepairOrderStateMachine`，用户端、师傅端和后台维修订单接口已接入统一动作校验；“待用户确认”继续使用状态 5 加完工时间派生，兼容历史订单。

目标：

- 集中管理维修订单状态、支付状态、售后状态和可执行动作。
- 避免用户端、师傅端、后台各自写状态判断导致逻辑不一致。

建议：

- 新增订单状态枚举，例如：
  - 待接单
  - 待上门
  - 待检测
  - 待支付
  - 服务中
  - 待用户确认
  - 已完成
  - 已取消
  - 已退款
- 新增领域服务，例如 `RepairOrderStateMachine`。
- 将以下逻辑从 Controller 中迁移出来：
  - 当前状态是否允许接单。
  - 是否允许扫码上门。
  - 是否允许提交检测。
  - 是否允许修改费用。
  - 是否允许用户支付尾款。
  - 是否允许确认完成或申请售后。

涉及模块：

- `back-end/src/main/java/com/example/backend/controller/user/UserOrdersController.java`
- `back-end/src/main/java/com/example/backend/controller/worker/WorkerOrderController.java`
- `back-end/src/main/java/com/example/backend/controller/admin/AdminReserveOrderController.java`

验证：

- 用单元测试覆盖每个状态的合法/非法动作。
- 对历史订单状态做兼容处理。

### 4. 拆分超大 Controller

实施结果：已提取 `RepairOrderQueryService`、`RepairOrderCommandService`，状态流转由状态机负责，资金、权限等跨端逻辑分别收口到领域 Service；原接口路径和响应 DTO 保持不变。

目标：

- Controller 只负责接收请求、调用服务、返回响应。
- 业务处理、DTO 组装、状态流转、消息通知独立到 Service。

优先拆分：

- `UserOrdersController`
- `WorkerOrderController`
- `AdminWorkerManageController`
- `UserProductOrderController`
- `AdminReserveOrderController`

建议拆出的服务：

- `RepairOrderQueryService`
- `RepairOrderCommandService`
- `RepairOrderPaymentService`
- `RepairOrderAfterSalesService`
- `RepairOrderNotificationService`
- `RepairOrderMediaService`

验证：

- 拆分前后接口响应字段保持一致。
- 编写核心接口回归测试。

### 5. 强化支付和资金一致性

实施结果：维修支付、退款、托管和师傅结算继续统一通过 `RepairOrderFundService`；资金流水新增确定性幂等键和数据库唯一约束，并新增每日资金核对任务。

目标：

- 订单支付记录、资金流水、余额变化之间可追踪、可校验。

建议：

- 明确资金流水和订单支付记录的一致性约束。
- 支付、退款、尾款支付、师傅收入入账都通过统一资金服务。
- 增加幂等键或业务唯一键，避免重复支付/重复记账。
- 后台增加资金异常核对页面或定时检查任务。

涉及模块：

- `RepairOrderFundService`
- `PaymentRecordsService`
- `FundFlowsService`
- `AccountBalancesService`
- 商品订单支付相关服务

验证：

- 对重复请求、并发支付、退款后再次支付等场景写测试。

## P1：权限和安全

> 完成状态：已于 2026-07-11 实施。

### 6. 细化后台权限模型

实施结果：已新增 `AdminDataScopeService`，统一校验门店、师傅、维修订单、评价和售后资源归属；门店管理员列表、详情和写操作均限制在本门店数据范围内。

目标：

- 平台管理员和门店管理员拥有不同数据边界。
- 门店管理员不能越权查看或操作其他门店的数据。

建议：

- 在 `AuthInterceptor` 角色校验基础上增加资源级权限校验。
- 后台接口按 `adminRole`、`storeId` 做数据过滤。
- 对门店、师傅、订单、评价、售后等核心数据增加权限断言。

验证：

- 使用测试 token 模拟不同管理员访问同一资源。
- 越权访问返回明确错误。

### 7. 增加 token 失效机制

实施结果：用户、师傅、管理员账号已增加 `tokenVersion`；JWT 签发和拦截器执行版本及账号状态校验，修改密码、封禁、注销和门店绑定变化会提升版本号。

目标：

- 用户改密码、注销、封禁、权限变更后，旧 token 不能继续使用。

建议：

- 账号表增加 tokenVersion 或 passwordChangedTime。
- JWT 中写入版本号。
- 拦截器校验 token 版本与账号当前版本一致。
- 注销、封禁、修改密码、管理员权限变更时提升版本号。

验证：

- 修改密码后旧 token 访问接口应失败。
- 封禁后旧 token 访问接口应失败。

### 8. 调整异常日志级别

实施结果：业务异常和参数异常使用 `warn`，数据库及未知异常使用 `error` 并保留堆栈；业务日志增加请求路径及可取得的 `orderId`、`accountId`。

目标：

- 线上日志中业务错误、参数错误和系统错误区分清楚。

建议：

- `BusinessException` 使用 `warn` 或 `info`。
- 参数校验错误使用 `warn`。
- 数据库异常、未知异常使用 `error`。
- 关键业务异常增加业务 ID，例如 orderId、accountId。

验证：

- 手动触发参数错误和系统错误，日志级别符合预期。

P1 自动验证结果：

- `mvnw.cmd spotless:check` 通过。
- `mvnw.cmd -DskipTests compile` 通过。
- `mvnw.cmd test` 通过；截至 2026-07-12 共 49 个测试，0 失败、0 错误。

## P2：测试覆盖

### 9. 补核心业务测试

> 进展：截至 2026-07-12 后端共 49 个测试通过，已覆盖维修状态机、权限和 token、资金幂等、信用处罚、支付意图、微信交易结果转换及回调失败重试语义。维修/商城外部支付、退款、数据库并发和端到端流程仍待补齐。

目标：

- 先覆盖最容易出事故的主流程，不追求一次性全量覆盖。

建议优先测试：

- 用户提交维修订单。
- 师傅接单。
- 师傅扫码上门。
- 师傅提交检测和报价。
- 用户支付尾款。
- 师傅提交完工。
- 用户确认完成。
- 用户取消订单。
- 用户申请售后。
- 管理员处理售后。
- 信用分不足时限制下单或接单。

建议技术方案：

- Service 层用单元测试覆盖状态机。
- Controller 层用 MockMvc 覆盖权限和接口响应。
- 数据库相关测试可使用测试库或 Testcontainers。

### 10. 补权限测试

目标：

- 明确 user、worker、admin 不能互相访问接口。

建议覆盖：

- 用户 token 访问 `/worker/**` 应失败。
- 师傅 token 访问 `/user/**` 应失败。
- 普通管理员访问其他门店数据应失败。
- 游客只能访问允许公开的商城浏览接口。

## P2：前端工程

### 11. 统一请求层和错误提示

> 完成状态：三端请求层已统一区分 401、403、业务错误和网络错误；401 清理登录状态，403 保留会话并展示权限原因。上传接口已有独立反馈处理。

目标：

- 用户端、师傅端、后台对 token 失效、权限不足、服务异常有一致处理。

建议：

- 对 `401` 统一跳登录。
- 对 `403` 统一提示无权限。
- 对业务错误展示后端 message。
- 对网络错误提供重试提示。
- 对上传接口单独处理进度和大小限制提示。

涉及模块：

- `admin-front-end/src/api/request.js`
- `user-front-end/api/request.js`
- `worker-front-end/api/request.js`

### 12. 拆分管理端大页面

目标：

- 降低页面维护成本。

优先拆分：

- `AdminOfflineOrderCreateView.vue`
- `AdminServiceConfigView.vue`
- `AdminUserDetailView.vue`
- `AdminProductCouponsView.vue`
- `AdminReserveOrderDetailView.vue`
- `AdminWorkerDetailView.vue`

建议拆分为：

- 筛选区组件
- 表格组件
- 表单弹窗组件
- 详情面板组件
- API hooks 或 composables

验证：

- 页面功能保持一致。
- 移动端或窄屏不出现布局错乱。

## P2：运营功能

### 13. 增加订单审计日志

目标：

- 每次订单关键状态变化都有记录，方便客服排查。

建议记录：

- 操作人
- 操作角色
- 原状态
- 新状态
- 操作原因
- 关联支付记录或售后记录
- 操作时间

可覆盖事件：

- 下单
- 接单
- 扫码上门
- 提交检测
- 修改费用
- 支付
- 完工
- 确认完成
- 取消
- 退款
- 售后申请和处理

### 14. 增加超时和提醒机制

目标：

- 减少订单卡在中间状态。

建议：

- 待接单超时提醒或自动取消。
- 师傅超过预约时间未上门提醒。
- 用户超过时间未支付尾款提醒。
- 售后待处理超时提醒后台。
- 信用分处罚与超时事件联动。

## P3：部署和可观测性

### 15. 完善健康检查和监控

目标：

- 线上可快速判断 API、数据库、Redis、OSS、短信服务是否正常。

建议：

- Actuator health 增加外部依赖检查。
- Prometheus 指标增加订单、支付、售后、上传失败等业务指标。
- Grafana 看板区分系统指标和业务指标。

### 16. 完善备份和恢复流程

目标：

- 不只备份，还要能验证恢复。

建议：

- MySQL 备份脚本增加保留策略。
- 定期恢复到测试库验证备份可用。
- 上传文件 OSS 和数据库记录做一致性抽检。

## 建议执行顺序

1. 固化构建、格式化、环境配置。
2. 抽订单状态机，并补状态机测试。
3. 拆分用户端和师傅端订单 Controller。
4. 强化支付/资金一致性。
5. 细化后台门店管理员权限。
6. 补前端统一请求错误处理。
7. 拆管理端大页面。
8. 增加订单审计日志和超时提醒。
