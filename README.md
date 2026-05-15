# 线上电器维修预约系统

基于 Spring Boot + Vue 的线上电器维修预约平台。

## 项目结构

本项目采用多分支管理，各模块代码位于不同分支：

| 分支 | 内容 |
|------|------|
| `main` | 项目文档、数据库 SQL |
| `backend` | Spring Boot 后端服务 |
| `admin-front-end` | Vue3 管理端 |
| `user-front-end` | 微信小程序用户端 |
| `worker-front-end` | Uni-app 维修师傅端 |

## 技术栈

- **后端**：Spring Boot 3 + MyBatis-Plus + MySQL + Redis
- **管理端**：Vue 3 + Element Plus
- **用户端**：微信小程序（Uni-app）
- **维修师傅端**：Uni-app + uView Plus

## 快速开始

1. 克隆仓库后，分别切换到各分支查看对应模块代码
2. 数据库初始化脚本位于 `sql/` 目录下
3. 后端配置文件需通过环境变量注入敏感信息（数据库密码、密钥等）

## 数据库

执行 `sql/` 目录下的 SQL 文件初始化数据库：

```bash
mysql -u root -p < sql/electrical_repair_shop.sql
```
