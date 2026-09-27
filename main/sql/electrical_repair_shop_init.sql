-- ==========================================================
-- electrical_repair_shop 初始化脚本（表结构 + 基础配置数据）
-- 用途：全新数据库导入后即可运行项目；不含用户/订单等业务样例数据。
-- 导入：mysql -uroot -p electrical_repair_shop < electrical_repair_shop_init.sql
-- 含全部建表（各迁移表已并入基线）：system_configs / service_categories /
--   service_types / fault_phenomena / product_categories / admin_accounts / admin_profiles
-- 默认管理员：username=admin / password=admin123456（登录后请立即修改）。
-- ==========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account_balances
-- ----------------------------
DROP TABLE IF EXISTS `account_balances`;
CREATE TABLE `account_balances`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，AB+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号ID',
  `account_type` int NOT NULL COMMENT '账号类型：1-用户，2-师傅，3-平台',
  `balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
  `frozen_balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
  `total_income` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总收入',
  `total_expense` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总支出',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account`(`account_id` ASC, `account_type` ASC) USING BTREE,
  INDEX `idx_account_type`(`account_type` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '账户余额表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for account_cancel_records
-- ----------------------------
DROP TABLE IF EXISTS `account_cancel_records`;
CREATE TABLE `account_cancel_records`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，CR+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号ID',
  `cancel_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '注销原因',
  `cancel_type` int NOT NULL COMMENT '注销类型：1-用户主动，2-系统注销',
  `operator_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人ID',
  `cancel_time` bigint NOT NULL COMMENT '注销时间戳',
  `data_retention_days` int NOT NULL DEFAULT 30 COMMENT '数据保留天数',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_cancel_type`(`cancel_type` ASC) USING BTREE,
  INDEX `idx_cancel_time`(`cancel_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '账号注销记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for admin_accounts
-- ----------------------------
DROP TABLE IF EXISTS `admin_accounts`;
CREATE TABLE `admin_accounts`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，AA+雪花ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `wechat_openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信OpenID',
  `wechat_unionid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信UnionID',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码哈希',
  `salt` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码盐值',
  `admin_type` int NOT NULL DEFAULT 2 COMMENT '管理员类型：1-超级管理员，2-普通管理员，3-客服',
  `admin_role` int NOT NULL DEFAULT 1 COMMENT '角色类型：1-超级管理员，2-门店管理员，3-客服',
  `permissions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '权限列表JSON',
  `account_status` int NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，2-冻结',
  `token_version` int NOT NULL DEFAULT 1 COMMENT '令牌版本号',
  `is_first_login` int NOT NULL DEFAULT 1 COMMENT '是否首次登录：0-否，1-是',
  `last_login_time` bigint NULL DEFAULT NULL COMMENT '最后登录时间戳',
  `last_login_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
  UNIQUE INDEX `uk_wechat_openid`(`wechat_openid` ASC) USING BTREE,
  INDEX `idx_wechat_unionid`(`wechat_unionid` ASC) USING BTREE,
  INDEX `idx_admin_type`(`admin_type` ASC) USING BTREE,
  INDEX `idx_account_status`(`account_status` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员账号表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin_accounts
-- ----------------------------
INSERT INTO `admin_accounts` VALUES ('AA296281039641382912', 'admin', '13800138000', '3129036103@qq.com', NULL, NULL, 'e2e526eff400afa0a99e355b3ad432c5b513f203a3dc18361401ab176166f480', '2XWl0mxi4eOUpqxD', 1, 1, '[]', 1, 1, 0, NULL, NULL, 1774706104486, 1774740612746, 4, 0);

-- ----------------------------
-- Table structure for admin_profiles
-- ----------------------------
DROP TABLE IF EXISTS `admin_profiles`;
CREATE TABLE `admin_profiles`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，AP+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `department` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '部门',
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职位',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account_id`(`account_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
  INDEX `idx_real_name`(`real_name` ASC) USING BTREE,
  INDEX `idx_department`(`department` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_admin_profiles_account_id` FOREIGN KEY (`account_id`) REFERENCES `admin_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin_profiles
-- ----------------------------
INSERT INTO `admin_profiles` VALUES ('AP296281039641382913', 'AA296281039641382912', '王小军', '13800138000', 'admin@repairshop.local', '人事部', '总经理', 1774706104486, 1774707251993, 3, 0);

-- ----------------------------
-- Table structure for after_sales_applications
-- ----------------------------
DROP TABLE IF EXISTS `after_sales_applications`;
CREATE TABLE `after_sales_applications`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，AS+雪花ID',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `order_type` int NOT NULL COMMENT '订单类型：1-维修订单，2-商品订单，3-钱包充值',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请用户账号ID',
  `application_type` int NOT NULL COMMENT '申请类型：1-退款，2-退货，3-换货，4-维修',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请原因',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '详细描述',
  `evidence_images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '证据图片JSON数组',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `contact_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系地址',
  `refund_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '退款金额',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-待审核，2-审核通过，3-审核拒绝，4-处理中，5-已完成，6-已取消',
  `admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理管理员ID',
  `admin_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '管理员备注',
  `processed_time` bigint NULL DEFAULT NULL COMMENT '处理时间戳',
  `completed_time` bigint NULL DEFAULT NULL COMMENT '完成时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_application_type`(`application_type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `fk_after_sales_applications_admin_id`(`admin_id` ASC) USING BTREE,
  CONSTRAINT `fk_after_sales_applications_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_after_sales_applications_admin_id` FOREIGN KEY (`admin_id`) REFERENCES `admin_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '售后申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for announcements
-- ----------------------------
DROP TABLE IF EXISTS `announcements`;
CREATE TABLE `announcements`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，AN+雪花ID',
  `channel` int NOT NULL DEFAULT 1 COMMENT '展示渠道：1-轮播图，2-公告栏',
  `content_type` int NOT NULL DEFAULT 2 COMMENT '内容类型：1-图片，2-文字',
  `title` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `subtitle` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '公告副标题',
  `content` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '公告内容',
  `emoji` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文字公告表情',
  `is_active` int NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `start_time` bigint NULL DEFAULT NULL COMMENT '展示开始时间戳（毫秒）',
  `end_time` bigint NULL DEFAULT NULL COMMENT '展示结束时间戳（毫秒）',
  `created_time` bigint NOT NULL COMMENT '创建时间戳（毫秒）',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳（毫秒）',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_channel`(`channel` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE,
  INDEX `idx_end_time`(`end_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '公告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cancel_reasons
-- ----------------------------
DROP TABLE IF EXISTS `cancel_reasons`;
CREATE TABLE `cancel_reasons`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，CLR+雪花ID',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `order_type` int NOT NULL COMMENT '订单类型：1-维修，2-商品',
  `reason_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原因编码',
  `reason_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原因标签',
  `user_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户手动备注',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order`(`order_id` ASC) USING BTREE,
  INDEX `idx_order_type`(`order_type` ASC) USING BTREE,
  INDEX `idx_reason_code`(`reason_code` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单取消原因表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for content_check_logs
-- ----------------------------
DROP TABLE IF EXISTS `content_check_logs`;
CREATE TABLE `content_check_logs`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，CCL+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提交人账号ID',
  `account_type` int NOT NULL COMMENT '提交人类型：1-用户，2-师傅，3-门店管理员',
  `content_type` int NOT NULL COMMENT '内容类型：1-文字，2-图片URL，3-视频URL',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始内容（文字或图片URL）',
  `check_result` int NOT NULL COMMENT '审核结果：1-通过，2-拦截，4-存疑放行',
  `hit_rule_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '阿里云建议（block/pass/watch）',
  `hit_keyword` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '阿里云标签',
  `source` int NOT NULL DEFAULT 2 COMMENT '检测来源：2-阿里云',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account`(`account_id` ASC, `account_type` ASC) USING BTREE,
  INDEX `idx_check_result`(`check_result` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '内容审核日志' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for conversation_messages
-- ----------------------------
DROP TABLE IF EXISTS `conversation_messages`;
CREATE TABLE `conversation_messages`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，CM+雪花ID',
  `session_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID，对应conversation_sessions.id',
  `sender_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发送人账号ID',
  `sender_type` int NOT NULL COMMENT '发送人类型：1-用户，2-师傅，3-管理员，4-系统',
  `receiver_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接收人账号ID',
  `receiver_type` int NOT NULL COMMENT '接收人类型：1-用户，2-师傅，3-管理员，4-系统',
  `content_type` int NOT NULL DEFAULT 1 COMMENT '内容类型：1-文本，2-图片，3-语音，4-系统提示，5-视频',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容（文本或JSON）',
  `extra_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '扩展字段JSON，如图片URL、语音时长等',
  `send_time` bigint NOT NULL COMMENT '发送时间戳',
  `read_time` bigint NULL DEFAULT NULL COMMENT '已读时间戳，未读为空',
  `status` int NOT NULL DEFAULT 1 COMMENT '消息状态：1-正常，2-已撤回，3-已删除',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_time`(`session_id` ASC, `send_time` ASC) USING BTREE,
  INDEX `idx_sender`(`sender_id` ASC, `sender_type` ASC, `send_time` ASC) USING BTREE,
  INDEX `idx_receiver_unread`(`receiver_id` ASC, `receiver_type` ASC, `status` ASC, `is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_msg_session` FOREIGN KEY (`session_id`) REFERENCES `conversation_sessions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内消息表（用户与师傅）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for conversation_sessions
-- ----------------------------
DROP TABLE IF EXISTS `conversation_sessions`;
CREATE TABLE `conversation_sessions`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，CS+雪花ID',
  `user_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID，对应user_accounts.id',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID，对应technician_accounts.id',
  `repair_order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联维修订单ID，对应repair_orders.id',
  `last_message_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后一条消息内容摘要',
  `last_message_time` bigint NULL DEFAULT NULL COMMENT '最后一条消息时间戳',
  `user_unread_count` int NOT NULL DEFAULT 0 COMMENT '用户未读消息数',
  `technician_unread_count` int NOT NULL DEFAULT 0 COMMENT '师傅未读消息数',
  `status` int NOT NULL DEFAULT 1 COMMENT '会话状态：1-正常，2-已关闭',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_technician_order`(`user_account_id` ASC, `technician_account_id` ASC, `repair_order_id` ASC) USING BTREE COMMENT '同一订单下用户与师傅只允许一个会话',
  INDEX `idx_user_account`(`user_account_id` ASC, `is_delete` ASC) USING BTREE,
  INDEX `idx_technician_account`(`technician_account_id` ASC, `is_delete` ASC) USING BTREE,
  INDEX `idx_last_message_time`(`last_message_time` ASC) USING BTREE,
  INDEX `fk_conv_repair_order`(`repair_order_id` ASC) USING BTREE,
  CONSTRAINT `fk_conv_repair_order` FOREIGN KEY (`repair_order_id`) REFERENCES `repair_orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_conv_technician_account` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_conv_user_account` FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内会话表（用户与师傅）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for coupons
-- ----------------------------
DROP TABLE IF EXISTS `coupons`;
CREATE TABLE `coupons`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，Q+雪花ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '优惠券名称',
  `type` int NOT NULL COMMENT '类型：1-满减券，2-折扣券，3-免费券',
  `discount_type` int NOT NULL COMMENT '优惠类型：1-固定金额，2-百分比折扣',
  `discount_value` decimal(10, 2) NOT NULL COMMENT '优惠值（金额或折扣百分比）',
  `min_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '最低消费金额',
  `max_discount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最大优惠金额',
  `total_count` int NOT NULL COMMENT '发放总数量',
  `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用数量',
  `per_user_limit` int NOT NULL DEFAULT 1 COMMENT '每人限领数量',
  `applicable_type` int NOT NULL DEFAULT 1 COMMENT '适用范围：1-全部，2-维修服务，3-商品购买',
  `applicable_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '适用商品/服务ID列表JSON',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-有效，2-已停用',
  `start_time` bigint NOT NULL COMMENT '开始时间戳',
  `end_time` bigint NOT NULL COMMENT '结束时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `store_id` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_time_range`(`start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '优惠券表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for credit_records
-- ----------------------------
DROP TABLE IF EXISTS `credit_records`;
CREATE TABLE `credit_records`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，CR+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号ID',
  `account_type` int NOT NULL COMMENT '账号类型：1-用户，2-师傅，3-门店管理员',
  `change_type` int NOT NULL COMMENT '变动类型：1-违规扣分，2-自动恢复，3-举报奖励，4-完单恢复，5-申诉恢复',
  `score_change` int NOT NULL COMMENT '积分变动（负数为扣分）',
  `score_before` int NOT NULL COMMENT '变动前积分',
  `score_after` int NOT NULL COMMENT '变动后积分',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '变动原因',
  `related_record_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联记录ID（处罚/举报/申诉ID）',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account`(`account_id` ASC, `account_type` ASC) USING BTREE,
  INDEX `idx_change_type`(`change_type` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '信用积分记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for fault_phenomena
-- ----------------------------
DROP TABLE IF EXISTS `fault_phenomena`;
CREATE TABLE `fault_phenomena`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，FP+雪花ID',
  `service_type_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务类型ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '故障现象名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '故障描述',
  `estimated_price_min` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '预估最低价格',
  `estimated_price_max` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '预估最高价格',
  `is_active` int NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_service_type_id`(`service_type_id` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_fault_phenomena_service_type_id` FOREIGN KEY (`service_type_id`) REFERENCES `service_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '故障现象表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fault_phenomena
-- ----------------------------
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000001', 'ST900000000000000001', '冰箱不制冷', '冰箱通电但冷藏或冷冻效果明显变差', 80.00, 260.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000002', 'ST900000000000000001', '冰箱制冷变弱', '冰箱可以运行，但制冷速度慢或温度不稳定', 60.00, 220.00, 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000003', 'ST900000000000000001', '冰箱异响', '压缩机或风扇运行时有明显异常噪音', 50.00, 180.00, 1, 3, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000004', 'ST900000000000000002', '油烟机吸力变小', '油烟机可以运行，但吸烟效果明显下降', 60.00, 180.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000005', 'ST900000000000000002', '油烟机无法启动', '按键操作后设备无反应或无法正常启动', 70.00, 220.00, 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000006', 'ST900000000000000002', '油烟机漏油', '设备在运行过程中出现明显漏油现象', 80.00, 240.00, 1, 3, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000007', 'ST900000000000000003', '洗衣机不排水', '洗衣结束后桶内积水无法正常排出', 70.00, 200.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000008', 'ST900000000000000003', '洗衣机不脱水', '洗衣机可以清洗，但无法进入脱水过程', 80.00, 230.00, 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000009', 'ST900000000000000003', '洗衣机异响', '设备运行时有明显晃动或异常碰撞声', 60.00, 180.00, 1, 3, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000010', 'ST900000000000000004', '新机安装', '适用于全新空调的首次上门安装', 120.00, 300.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000011', 'ST900000000000000004', '移机重装', '适用于旧空调拆机后重新安装', 180.00, 420.00, 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000012', 'ST900000000000000005', '新机安装', '适用于新购油烟机的标准安装服务', 100.00, 260.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000013', 'ST900000000000000005', '旧机拆装', '适用于旧机拆除并安装新机的服务场景', 120.00, 300.00, 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000014', 'ST900000000000000006', '电饭煲无法通电', '电饭煲插电后面板无反应', 30.00, 120.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000015', 'ST900000000000000006', '电饭煲加热异常', '电饭煲可以启动，但无法正常加热或保温', 40.00, 150.00, 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000016', 'ST900000000000000007', '微波炉不加热', '微波炉可以启动，但食物无法加热', 50.00, 180.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `fault_phenomena` VALUES ('FP900000000000000017', 'ST900000000000000007', '微波炉按键失灵', '控制面板按键失效或响应异常', 40.00, 140.00, 1, 2, 1774741181384, 1774741181384, 1, 0);

-- ----------------------------
-- Table structure for files
-- ----------------------------
DROP TABLE IF EXISTS `files`;
CREATE TABLE `files`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，FI+雪花ID',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件路径',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问URL',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MIME类型',
  `file_extension` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件扩展名',
  `uploader_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上传者ID',
  `uploader_type` int NOT NULL COMMENT '上传者类型：1-用户，2-师傅，3-管理员',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务ID',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uploader`(`uploader_id` ASC, `uploader_type` ASC) USING BTREE,
  INDEX `idx_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_file_extension`(`file_extension` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for fund_flows
-- ----------------------------
DROP TABLE IF EXISTS `fund_flows`;
CREATE TABLE `fund_flows`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，FF+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号ID',
  `account_type` int NOT NULL COMMENT '账号类型：1-用户，2-师傅，3-平台',
  `flow_type` int NOT NULL COMMENT '流水类型：1-收入，2-支出',
  `amount` decimal(10, 2) NOT NULL COMMENT '金额',
  `balance_before` decimal(10, 2) NOT NULL COMMENT '变动前余额',
  `balance_after` decimal(10, 2) NOT NULL COMMENT '变动后余额',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型',
  `business_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务ID',
  `idempotency_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务幂等键',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '流水描述',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_fund_flow_idempotency`(`idempotency_key` ASC) USING BTREE,
  INDEX `idx_account`(`account_id` ASC, `account_type` ASC) USING BTREE,
  INDEX `idx_flow_type`(`flow_type` ASC) USING BTREE,
  INDEX `idx_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  CONSTRAINT `fk_fund_flows_account_balance` FOREIGN KEY (`account_id`, `account_type`) REFERENCES `account_balances` (`account_id`, `account_type`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资金流水表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for image_review_queue
-- ----------------------------
DROP TABLE IF EXISTS `image_review_queue`;
CREATE TABLE `image_review_queue`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，IRQ+雪花ID',
  `image_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联图片或视频的文件ID',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型：AVATAR/STORE/GOODS/CASE/VIDEO/FAULT',
  `business_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务ID',
  `status` int NOT NULL DEFAULT 1 COMMENT '审核状态：1-待审核，2-审核通过，3-审核拒绝，4-存疑放行',
  `reviewer_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核人ID',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拒绝原因',
  `review_time` bigint NULL DEFAULT NULL COMMENT '审核时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_image_id`(`image_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_business_type`(`business_type` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图片/视频审核队列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for images
-- ----------------------------
DROP TABLE IF EXISTS `images`;
CREATE TABLE `images`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，IM+雪花ID',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件路径',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问URL',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MIME类型',
  `width` int NULL DEFAULT NULL COMMENT '图片宽度',
  `height` int NULL DEFAULT NULL COMMENT '图片高度',
  `uploader_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上传者ID',
  `uploader_type` int NOT NULL COMMENT '上传者类型：1-用户，2-师傅，3-管理员',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务ID',
  `review_status` int NOT NULL DEFAULT 2 COMMENT '审核状态：1-待审核，2-通过，3-拒绝',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uploader`(`uploader_id` ASC, `uploader_type` ASC) USING BTREE,
  INDEX `idx_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图片表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for operation_logs
-- ----------------------------
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，OL+雪花ID',
  `operator_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人ID',
  `operator_type` int NOT NULL COMMENT '操作人类型：1-用户，2-师傅，3-管理员，4-系统',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
  `operation_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作描述',
  `module_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求参数',
  `response_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '响应数据',
  `ip_address` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户代理',
  `device_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设备标识ID',
  `execution_time` int NULL DEFAULT NULL COMMENT '执行时间（毫秒）',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-成功，2-失败',
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误信息',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operator`(`operator_id` ASC, `operator_type` ASC) USING BTREE,
  INDEX `idx_operation_type`(`operation_type` ASC) USING BTREE,
  INDEX `idx_module_name`(`module_name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_ip_address`(`ip_address` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_operation_logs_composite`(`operator_id` ASC, `operator_type` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_device_id`(`device_id` ASC) USING BTREE,
  INDEX `idx_operator_device_operation`(`operator_id` ASC, `device_id` ASC, `operation_type` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for order_door_qr_codes
-- ----------------------------
DROP TABLE IF EXISTS `order_door_qr_codes`;
CREATE TABLE `order_door_qr_codes`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键：ODQ+雪花ID',
  `repair_order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '维修订单ID（repair_orders.id）',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID（technician_accounts.id）',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID（user_accounts.id）',
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '二维码令牌',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-未使用，2-已使用，3-已失效',
  `expire_time` bigint NOT NULL COMMENT '失效时间戳（毫秒）',
  `used_time` bigint NULL DEFAULT NULL COMMENT '核销时间戳（毫秒）',
  `used_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '核销人ID（账号ID）',
  `image_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '二维码图片ID（images.id，PNG）',
  `created_time` bigint NOT NULL COMMENT '创建时间戳（毫秒）',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳（毫秒）',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_door_qr_token`(`token` ASC) USING BTREE,
  INDEX `idx_order_door_qr_order_id`(`repair_order_id` ASC) USING BTREE,
  INDEX `idx_order_door_qr_status`(`status` ASC) USING BTREE,
  INDEX `idx_order_door_qr_expire_time`(`expire_time` ASC) USING BTREE,
  INDEX `idx_order_door_qr_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `fk_order_door_qr_account_id`(`account_id` ASC) USING BTREE,
  INDEX `fk_order_door_qr_image_id`(`image_id` ASC) USING BTREE,
  INDEX `fk_order_door_qr_technician_id`(`technician_account_id` ASC) USING BTREE,
  CONSTRAINT `fk_order_door_qr_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_door_qr_image_id` FOREIGN KEY (`image_id`) REFERENCES `images` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_door_qr_order_id` FOREIGN KEY (`repair_order_id`) REFERENCES `repair_orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_door_qr_technician_id` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '上门二维码表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for order_items
-- ----------------------------
DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，OI+雪花ID',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `product_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品ID',
  `product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `product_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品图片',
  `product_price` decimal(10, 2) NOT NULL COMMENT '商品单价',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_price` decimal(10, 2) NOT NULL COMMENT '小计金额',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_order_items_order_id` FOREIGN KEY (`order_id`) REFERENCES `product_orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_items_product_id` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单商品表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for order_progress
-- ----------------------------
DROP TABLE IF EXISTS `order_progress`;
CREATE TABLE `order_progress`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，OP+雪花ID',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `status` int NOT NULL COMMENT '状态',
  `status_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态描述',
  `operator_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_type` int NULL DEFAULT NULL COMMENT '操作人类型：1-用户，2-师傅，3-管理员，4-系统',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_operator`(`operator_id` ASC, `operator_type` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  CONSTRAINT `fk_order_progress_order_id` FOREIGN KEY (`order_id`) REFERENCES `repair_orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单进度表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for recharge_orders
-- ----------------------------
DROP TABLE IF EXISTS `recharge_orders`;
CREATE TABLE `recharge_orders`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，RC+雪花ID',
  `recharge_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '充值订单号',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `amount` decimal(10, 2) NOT NULL COMMENT '充值金额',
  `currency` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `provider` int NOT NULL COMMENT '支付渠道：1-微信，2-支付宝',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-待支付，2-支付中，3-成功，4-失败，6-关闭',
  `expires_at` bigint NOT NULL COMMENT '过期时间戳',
  `posted_time` bigint NULL DEFAULT NULL COMMENT '入账时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_recharge_no`(`recharge_no` ASC) USING BTREE,
  INDEX `idx_recharge_account`(`account_id` ASC) USING BTREE,
  INDEX `idx_recharge_status_expiry`(`status` ASC, `expires_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '钱包充值订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for payment_refunds
-- ----------------------------
DROP TABLE IF EXISTS `payment_refunds`;
CREATE TABLE `payment_refunds`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '退款单ID，RF+雪花ID',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台退款单号',
  `payment_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原支付记录ID',
  `payment_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原支付单号',
  `account_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `refund_amount` decimal(10, 2) NOT NULL COMMENT '退款金额',
  `currency` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `refund_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '退款原因',
  `refund_status` tinyint NOT NULL DEFAULT 1 COMMENT '退款状态：1-退款中，2-退款成功，3-退款失败',
  `provider` tinyint NOT NULL COMMENT '支付渠道：1-微信，2-支付宝',
  `provider_refund_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '渠道退款单号',
  `idempotency_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '幂等键，防止重复退款',
  `callback_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '脱敏后的退款回调数据',
  `initiated_time` bigint NOT NULL COMMENT '退款发起时间戳',
  `completed_time` bigint NULL DEFAULT NULL COMMENT '退款完成时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_refund_no`(`refund_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_idempotency_key`(`idempotency_key` ASC) USING BTREE,
  INDEX `idx_payment_id`(`payment_id` ASC) USING BTREE,
  INDEX `idx_refund_account`(`account_id` ASC) USING BTREE,
  INDEX `idx_refund_status`(`refund_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支付退款记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for reconciliation_batches
-- ----------------------------
DROP TABLE IF EXISTS `reconciliation_batches`;
CREATE TABLE `reconciliation_batches`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，RB+雪花ID',
  `batch_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次号',
  `batch_type` int NOT NULL COMMENT '批次类型：1-增量，2-全量',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-存在异常，3-执行失败',
  `window_start` bigint NULL DEFAULT NULL COMMENT '扫描窗口开始时间戳',
  `window_end` bigint NULL DEFAULT NULL COMMENT '扫描窗口结束时间戳',
  `scanned_count` int NOT NULL DEFAULT 0 COMMENT '扫描对象数',
  `issue_count` int NOT NULL DEFAULT 0 COMMENT '异常数',
  `error_summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误摘要',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_batch_no`(`batch_no` ASC) USING BTREE,
  INDEX `idx_batch_window_end`(`window_end` ASC) USING BTREE,
  INDEX `idx_batch_created`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资金对账批次表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for reconciliation_issues
-- ----------------------------
DROP TABLE IF EXISTS `reconciliation_issues`;
CREATE TABLE `reconciliation_issues`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，RI+雪花ID',
  `batch_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属批次ID',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '异常分类',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务对象类型',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务对象ID',
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '异常描述',
  `status` int NOT NULL DEFAULT 1 COMMENT '处理状态：1-待处理，2-已处理，3-已忽略',
  `handled_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理人账号ID',
  `handled_time` bigint NULL DEFAULT NULL COMMENT '处理时间戳',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理备注',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_issue_batch`(`batch_id` ASC) USING BTREE,
  INDEX `idx_issue_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资金对账异常明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for scheduler_locks
-- ----------------------------
DROP TABLE IF EXISTS `scheduler_locks`;
CREATE TABLE `scheduler_locks`  (
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '锁名（全局唯一）',
  `locked_until` bigint NOT NULL DEFAULT 0 COMMENT '锁持有截止时间戳',
  `locked_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '持锁实例',
  `updated_time` bigint NOT NULL DEFAULT 0 COMMENT '更新时间戳',
  PRIMARY KEY (`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '定时任务分布式锁' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for audit_events
-- ----------------------------
DROP TABLE IF EXISTS `audit_events`;
CREATE TABLE `audit_events`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，AE+雪花ID',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务对象类型',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务对象ID',
  `before_state` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '变更前状态',
  `after_state` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '变更后状态',
  `amount_before` decimal(12, 2) NULL DEFAULT NULL COMMENT '金额变更前',
  `amount_after` decimal(12, 2) NULL DEFAULT NULL COMMENT '金额变更后',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作原因',
  `related_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联单号',
  `operator_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作者账号ID',
  `operator_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作者角色',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作者名称',
  `source_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源IP',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求ID',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_biz`(`biz_type` ASC, `biz_id` ASC) USING BTREE,
  INDEX `idx_audit_created`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '统一业务审计事件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for technician_withdrawals
-- ----------------------------
DROP TABLE IF EXISTS `technician_withdrawals`;
CREATE TABLE `technician_withdrawals`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TW+雪花ID',
  `withdrawal_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提现单号',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '维修人员账号ID',
  `amount` decimal(12, 2) NOT NULL COMMENT '提现金额',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-待审核，2-已通过待打款，3-打款成功，4-打款失败已退回，5-已驳回',
  `payout_account` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收款账户说明',
  `idempotency_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '客户端幂等键',
  `review_admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核管理员账号ID',
  `review_time` bigint NULL DEFAULT NULL COMMENT '审核时间戳',
  `review_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核备注',
  `payout_provider_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '渠道/转账单号',
  `payout_time` bigint NULL DEFAULT NULL COMMENT '打款时间戳',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败原因',
  `requires_review` int NOT NULL DEFAULT 0 COMMENT '需高金额复核：0-否，1-是',
  `review_confirmed` int NOT NULL DEFAULT 0 COMMENT '高金额复核已确认：0-否，1-是',
  `risk_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '风险说明',
  `intercepted` int NOT NULL DEFAULT 0 COMMENT '人工拦截：0-否，1-是',
  `intercept_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拦截原因',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_withdrawal_no`(`withdrawal_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_withdrawal_idempotency`(`idempotency_key` ASC) USING BTREE,
  INDEX `idx_withdrawal_tech`(`technician_account_id` ASC) USING BTREE,
  INDEX `idx_withdrawal_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维修人员提现单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for notification_outbox
-- ----------------------------
DROP TABLE IF EXISTS `notification_outbox`;
CREATE TABLE `notification_outbox`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，NO+雪花ID',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型',
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发送渠道：IN_APP/SMS/WECHAT/EMAIL',
  `receiver_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '接收人账号ID',
  `receiver_type` int NULL DEFAULT NULL COMMENT '接收人类型：1-用户，2-师傅，3-管理员',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通知标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通知内容',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模板编码',
  `template_version` int NULL DEFAULT NULL COMMENT '模板版本',
  `payload` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模板变量（JSON）',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务对象类型',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务对象ID',
  `dedup_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '去重唯一键，防止重复发送',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-待发送，2-发送中，3-已发送，4-待重试，5-死信',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry` int NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  `next_retry_time` bigint NOT NULL DEFAULT 0 COMMENT '下次发送时间戳',
  `last_error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最近一次失败原因',
  `sent_time` bigint NULL DEFAULT NULL COMMENT '发送成功时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_outbox_dedup`(`dedup_key` ASC) USING BTREE,
  INDEX `idx_outbox_due`(`status` ASC, `next_retry_time` ASC) USING BTREE,
  INDEX `idx_outbox_created`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '可靠通知事务 outbox' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for order_sla_events
-- ----------------------------
DROP TABLE IF EXISTS `order_sla_events`;
CREATE TABLE `order_sla_events`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，SL+雪花ID',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `order_type` int NOT NULL DEFAULT 1 COMMENT '订单类型：1-维修订单，2-商品订单',
  `sla_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SLA类型：ACCEPT/VISIT/INSPECTION/PAYMENT/COMPLETION',
  `order_status` int NULL DEFAULT NULL COMMENT '触发时订单状态',
  `responsible_party` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '责任方：PLATFORM/TECHNICIAN/USER',
  `responsible_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '责任方账号ID',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-处理中，2-已恢复，3-已自动处理，4-已忽略',
  `escalation_level` int NOT NULL DEFAULT 0 COMMENT '升级级别：0-无，1-已提醒，2-已升级',
  `notify_count` int NOT NULL DEFAULT 0 COMMENT '通知次数',
  `action_taken` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '已执行动作：REMIND/ESCALATE/AUTO_CANCEL',
  `overdue_minutes` int NOT NULL DEFAULT 0 COMMENT '最近一次超时时长（分钟）',
  `first_exceeded_time` bigint NOT NULL COMMENT '首次超时时间戳',
  `last_notified_time` bigint NULL DEFAULT NULL COMMENT '最近通知时间戳',
  `resolved_time` bigint NULL DEFAULT NULL COMMENT '恢复/处理完成时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sla_order`(`order_id` ASC, `sla_type` ASC) USING BTREE,
  INDEX `idx_sla_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单 SLA 超时事件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for support_tickets
-- ----------------------------
DROP TABLE IF EXISTS `support_tickets`;
CREATE TABLE `support_tickets`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TK+雪花ID',
  `ticket_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工单号',
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工单分类',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内容',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联业务类型：REPAIR_ORDER/PRODUCT_ORDER/PAYMENT/REFUND/AFTER_SALES/ACCOUNT',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联业务ID',
  `related_user_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联用户账号ID',
  `related_technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联师傅账号ID',
  `priority` int NOT NULL DEFAULT 2 COMMENT '优先级：1-高，2-中，3-低',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-待处理，2-处理中，3-待审批，4-已解决，5-已关闭',
  `source` int NOT NULL DEFAULT 1 COMMENT '来源：1-后台创建，2-用户提交，3-系统',
  `assignee_admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前处理人账号ID',
  `creator_admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人账号ID',
  `amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '关联金额（用于阈值审批）',
  `requires_approval` int NOT NULL DEFAULT 0 COMMENT '是否需二次确认/双人审批：0-否，1-是',
  `approver_admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批人账号ID',
  `approve_time` bigint NULL DEFAULT NULL COMMENT '审批时间戳',
  `due_time` bigint NULL DEFAULT NULL COMMENT '处理时限时间戳',
  `resolved_time` bigint NULL DEFAULT NULL COMMENT '解决时间戳',
  `closed_time` bigint NULL DEFAULT NULL COMMENT '关闭时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_no`(`ticket_no` ASC) USING BTREE,
  INDEX `idx_ticket_status`(`status` ASC) USING BTREE,
  INDEX `idx_ticket_assignee`(`assignee_admin_id` ASC) USING BTREE,
  INDEX `idx_ticket_biz`(`biz_type` ASC, `biz_id` ASC) USING BTREE,
  INDEX `idx_ticket_created`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '客服工单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for support_ticket_logs
-- ----------------------------
DROP TABLE IF EXISTS `support_ticket_logs`;
CREATE TABLE `support_ticket_logs`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TL+雪花ID',
  `ticket_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工单ID',
  `action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作：CREATE/ASSIGN/PRIORITY/STATUS/COMMENT/APPROVE/REJECT/ATTACH',
  `operator_admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人账号ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人名称',
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作内容/备注',
  `from_status` int NULL DEFAULT NULL COMMENT '变更前状态',
  `to_status` int NULL DEFAULT NULL COMMENT '变更后状态',
  `attachments` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附件（JSON 数组）',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ticket_log_ticket`(`ticket_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '客服工单操作记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for appointment_reservations
-- ----------------------------
DROP TABLE IF EXISTS `appointment_reservations`;
CREATE TABLE `appointment_reservations`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，AR+雪花ID',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID',
  `appointment_time` bigint NOT NULL COMMENT '预约时间戳',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '占用订单ID',
  `active_slot` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '有效占用键（师傅:时间戳），释放后置 NULL',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-占用中，2-已释放',
  `release_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '释放原因',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_reservation_order`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_reservation_active`(`active_slot` ASC) USING BTREE,
  INDEX `idx_reservation_tech_time`(`technician_account_id` ASC, `appointment_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预约时段占用表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for appointment_closures
-- ----------------------------
DROP TABLE IF EXISTS `appointment_closures`;
CREATE TABLE `appointment_closures`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，AC+雪花ID',
  `owner_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '范围：GLOBAL-全平台，TECHNICIAN-师傅，STORE-门店',
  `owner_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '范围对象ID（GLOBAL 可为空）',
  `start_time` bigint NOT NULL COMMENT '开始时间戳',
  `end_time` bigint NOT NULL COMMENT '结束时间戳',
  `reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原因（停业/节假日/请假）',
  `operator_admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作管理员ID',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_closure_window`(`start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `idx_closure_owner`(`owner_type` ASC, `owner_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预约停业/请假时段表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for invoices
-- ----------------------------
DROP TABLE IF EXISTS `invoices`;
CREATE TABLE `invoices`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，IV+雪花ID',
  `invoice_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发票申请号',
  `user_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请用户账号ID',
  `order_type` int NOT NULL COMMENT '订单类型：1-维修订单，2-商品订单',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `amount` decimal(12, 2) NOT NULL COMMENT '开票金额',
  `tax_rate` decimal(6, 4) NOT NULL DEFAULT 0.0000 COMMENT '税率',
  `tax_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '税额',
  `title_type` int NOT NULL DEFAULT 1 COMMENT '抬头类型：1-个人，2-企业',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发票抬头',
  `taxpayer_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '纳税人识别号',
  `content_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开票内容',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-待开票，2-已开票，3-已红冲，4-已驳回',
  `apply_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请备注',
  `invoice_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开票结果/PDF地址',
  `issue_time` bigint NULL DEFAULT NULL COMMENT '开票时间戳',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `red_time` bigint NULL DEFAULT NULL COMMENT '红冲时间戳',
  `red_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '红冲原因',
  `operator_admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作管理员ID',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_invoice_no`(`invoice_no` ASC) USING BTREE,
  INDEX `idx_invoice_user`(`user_account_id` ASC) USING BTREE,
  INDEX `idx_invoice_order`(`order_type` ASC, `order_id` ASC) USING BTREE,
  INDEX `idx_invoice_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '发票表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for finance_order_snapshots
-- ----------------------------
DROP TABLE IF EXISTS `finance_order_snapshots`;
CREATE TABLE `finance_order_snapshots`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，FO+雪花ID',
  `order_type` int NOT NULL COMMENT '订单类型：1-维修订单，2-商品订单',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `order_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '订单号',
  `user_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户账号ID',
  `income_service` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '维修服务费收入',
  `income_material` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '配件收入',
  `income_product` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '商品收入',
  `income_door_fee` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '上门费收入',
  `income_shipping` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '运费收入',
  `income_platform_service` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '平台服务费收入',
  `total_paid` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '实收合计',
  `total_refunded` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '退款合计',
  `net_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '净额（实收-退款）',
  `platform_commission` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '平台佣金',
  `technician_income` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '维修人员应结收入',
  `tax_rate` decimal(6, 4) NOT NULL DEFAULT 0.0000 COMMENT '税率',
  `tax_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '税额',
  `discount_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `discount_bearer` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '优惠承担方：PLATFORM/MERCHANT/TECHNICIAN',
  `pay_time` bigint NULL DEFAULT NULL COMMENT '支付时间戳',
  `perform_time` bigint NULL DEFAULT NULL COMMENT '履约完成时间戳',
  `refund_time` bigint NULL DEFAULT NULL COMMENT '退款时间戳',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-正常',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_finance_order`(`order_type` ASC, `order_id` ASC) USING BTREE,
  INDEX `idx_finance_pay`(`pay_time` ASC) USING BTREE,
  INDEX `idx_finance_perform`(`perform_time` ASC) USING BTREE,
  INDEX `idx_finance_refund`(`refund_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单财务核算快照表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for payment_records
-- ----------------------------
DROP TABLE IF EXISTS `payment_records`;
CREATE TABLE `payment_records`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，PR+雪花ID',
  `payment_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付单号',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `order_type` int NOT NULL COMMENT '订单类型：1-维修订单，2-商品订单',
  `biz_stage` int NULL DEFAULT NULL COMMENT '支付阶段（订单支付专用）：1-预付，2-尾款，3-商品订单全额',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `payment_method` int NOT NULL COMMENT '支付方式：1-微信支付，2-支付宝，3-现金，4-银行卡，5-钱包支付',
  `payment_amount` decimal(10, 2) NOT NULL COMMENT '支付金额',
  `currency` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `payment_status` int NOT NULL DEFAULT 1 COMMENT '支付状态：1-待支付，2-支付中，3-支付成功，4-支付失败，5-已退款，6-已关闭',
  `third_party_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '第三方支付单号',
  `expires_at` bigint NULL DEFAULT NULL COMMENT '支付意图过期时间戳',
  `payment_time` bigint NULL DEFAULT NULL COMMENT '支付时间戳',
  `refund_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `refund_time` bigint NULL DEFAULT NULL COMMENT '退款时间戳',
  `refund_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '退款原因',
  `callback_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '回调数据',
  `callback_processed_time` bigint NULL DEFAULT NULL COMMENT '首次成功处理回调时间戳',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_payment_no`(`payment_no` ASC) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_order_type`(`order_type` ASC) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_payment_method`(`payment_method` ASC) USING BTREE,
  INDEX `idx_payment_status`(`payment_status` ASC) USING BTREE,
  UNIQUE INDEX `uk_third_party_no`(`third_party_no` ASC) USING BTREE,
  INDEX `idx_payment_time`(`payment_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支付记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for penalty_records
-- ----------------------------
DROP TABLE IF EXISTS `penalty_records`;
CREATE TABLE `penalty_records`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，PN+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '被处罚账号ID',
  `account_type` int NOT NULL COMMENT '账号类型：1-用户，2-师傅，3-门店管理员',
  `report_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联举报ID',
  `violation_level` int NOT NULL COMMENT '违规等级：1-4',
  `score_deducted` int NOT NULL COMMENT '扣分分值',
  `penalty_type` int NOT NULL COMMENT '处罚类型：1-警告，2-功能限制，3-封禁',
  `restricted_functions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '被限制的功能JSON',
  `ban_duration_hours` int NULL DEFAULT NULL COMMENT '封禁时长（小时），NULL为永久',
  `ban_start_time` bigint NULL DEFAULT NULL COMMENT '封禁开始时间',
  `ban_end_time` bigint NULL DEFAULT NULL COMMENT '封禁结束时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-执行中，2-已解除，3-已过期',
  `operator_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `appeal_status` int NOT NULL DEFAULT 0 COMMENT '申诉状态：0-未申诉，1-申诉中，2-申诉通过，3-申诉驳回',
  `appeal_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申诉原因',
  `appeal_time` bigint NULL DEFAULT NULL COMMENT '申诉时间戳',
  `appeal_result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申诉结果',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account`(`account_id` ASC, `account_type` ASC) USING BTREE,
  INDEX `idx_report_id`(`report_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_appeal_status`(`appeal_status` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_penalty_report_id` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '处罚记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for product_categories
-- ----------------------------
DROP TABLE IF EXISTS `product_categories`;
CREATE TABLE `product_categories`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，PC+雪花ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `parent_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '父分类ID',
  `level` int NOT NULL DEFAULT 1 COMMENT '分类层级',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '分类描述',
  `icon_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标URL',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `is_active` int NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of product_categories
-- ----------------------------
INSERT INTO `product_categories` VALUES ('PC900000000000000001', '家电商城', NULL, 1, '测试用一级分类：家电商城', NULL, 1, 1, 1774746449130, 1774746449130, 1, 0);
INSERT INTO `product_categories` VALUES ('PC900000000000000002', '净水与滤芯', 'PC900000000000000001', 2, '测试用二级分类：净水与滤芯', NULL, 1, 1, 1774746449130, 1774746449130, 1, 0);
INSERT INTO `product_categories` VALUES ('PC900000000000000003', '净水器滤芯', 'PC900000000000000002', 3, '测试用三级分类：净水器滤芯', NULL, 1, 1, 1774746449130, 1774746449130, 1, 0);
INSERT INTO `product_categories` VALUES ('PC900000000000000004', '清洁配件', 'PC900000000000000001', 2, '测试用二级分类：清洁配件', NULL, 2, 1, 1774746449130, 1774746449130, 1, 0);
INSERT INTO `product_categories` VALUES ('PC900000000000000005', '吸尘器配件', 'PC900000000000000004', 3, '测试用三级分类：吸尘器配件', NULL, 1, 1, 1774746449130, 1774746449130, 1, 0);
INSERT INTO `product_categories` VALUES ('PC900000000000000006', '二手整机', NULL, 1, '测试用一级分类：二手整机', NULL, 2, 1, 1774746449130, 1774746449130, 1, 0);
INSERT INTO `product_categories` VALUES ('PC900000000000000007', '二手厨房电器', 'PC900000000000000006', 2, '测试用二级分类：二手厨房电器', NULL, 1, 1, 1774746449130, 1774746449130, 1, 0);
INSERT INTO `product_categories` VALUES ('PC900000000000000008', '二手微波炉', 'PC900000000000000007', 3, '测试用三级分类：二手微波炉', NULL, 1, 1, 1774746449130, 1774746449130, 1, 0);

-- ----------------------------
-- Table structure for product_favorites
-- ----------------------------
DROP TABLE IF EXISTS `product_favorites`;
CREATE TABLE `product_favorites`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，PF+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `product_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品ID',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account_product`(`account_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  CONSTRAINT `fk_product_favorites_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_product_favorites_product_id` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品收藏表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for product_orders
-- ----------------------------
DROP TABLE IF EXISTS `product_orders`;
CREATE TABLE `product_orders`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，PO+雪花ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `order_status` int NOT NULL DEFAULT 1 COMMENT '订单状态：1-待支付，2-待发货，3-待收货，4-待评价，5-已完成，6-已取消，7-已退款',
  `payment_status` int NOT NULL DEFAULT 1 COMMENT '支付状态：1-待支付，2-已支付，3-已退款',
  `delivery_status` int NOT NULL DEFAULT 1 COMMENT '配送状态：1-待发货，2-已发货，3-配送中，4-已送达',
  `total_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `product_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '商品金额',
  `shipping_fee` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '运费',
  `discount_amount` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `actual_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '实际支付金额',
  `coupon_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的优惠券ID',
  `payment_method` int NULL DEFAULT NULL COMMENT '支付方式：1-微信支付，2-支付宝，3-银行卡',
  `payment_time` bigint NULL DEFAULT NULL COMMENT '支付时间戳',
  `delivery_address_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货地址ID',
  `delivery_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货人姓名',
  `delivery_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货人电话',
  `delivery_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货地址',
  `delivery_company` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '快递公司',
  `delivery_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '快递单号',
  `delivery_time` bigint NULL DEFAULT NULL COMMENT '发货时间戳',
  `receive_time` bigint NULL DEFAULT NULL COMMENT '收货时间戳',
  `completion_time` bigint NULL DEFAULT NULL COMMENT '完成时间戳',
  `cancel_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '取消原因',
  `cancel_time` bigint NULL DEFAULT NULL COMMENT '取消时间戳',
  `cancel_reason_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联取消原因ID',
  `fulfillment_type` int NULL DEFAULT NULL COMMENT '履约方式：1-自取，2-送货上门',
  `refund_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '退款原因',
  `refund_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `refund_time` bigint NULL DEFAULT NULL COMMENT '退款时间戳',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '订单备注',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_order_status`(`order_status` ASC) USING BTREE,
  INDEX `idx_payment_status`(`payment_status` ASC) USING BTREE,
  INDEX `idx_delivery_status`(`delivery_status` ASC) USING BTREE,
  INDEX `idx_payment_time`(`payment_time` ASC) USING BTREE,
  INDEX `idx_delivery_time`(`delivery_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `fk_product_orders_delivery_address_id`(`delivery_address_id` ASC) USING BTREE,
  INDEX `idx_product_orders_composite`(`order_status` ASC, `payment_status` ASC, `created_time` ASC) USING BTREE,
  CONSTRAINT `fk_product_orders_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_product_orders_delivery_address_id` FOREIGN KEY (`delivery_address_id`) REFERENCES `user_addresses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for products
-- ----------------------------
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，PD+雪花ID',
  `product_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品编号',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `product_type` int NOT NULL DEFAULT 1 COMMENT '商品类型：1-普通商品，2-二手商品',
  `category_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类ID',
  `store_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '归属门店ID',
  `fulfillment_type` int NULL DEFAULT NULL COMMENT '履约方式：1-自取，2-送货上门',
  `delivery_range_km` decimal(8, 2) NULL DEFAULT NULL COMMENT '配送范围（公里）',
  `delivery_fee` decimal(8, 2) NULL DEFAULT NULL COMMENT '配送费',
  `need_appointment` int NULL DEFAULT 0 COMMENT '送货上门是否需要预约：0-否，1-是',
  `audit_status` int NOT NULL DEFAULT 1 COMMENT '审核状态：1-待审核，2-审核通过，3-审核拒绝',
  `is_frozen` int NOT NULL DEFAULT 0 COMMENT '是否冻结：0-正常，1-已冻结',
  `frozen_time` bigint NULL DEFAULT NULL COMMENT '冻结时间戳',
  `frozen_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '冻结操作人ID',
  `brand` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '品牌',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '型号',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品描述',
  `specifications` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '规格参数JSON',
  `main_image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主图URL',
  `image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品图片JSON数组',
  `video_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品视频JSON数组',
  `original_price` decimal(10, 2) NOT NULL COMMENT '原价',
  `selling_price` decimal(10, 2) NOT NULL COMMENT '售价',
  `cost_price` decimal(10, 2) NOT NULL COMMENT '成本价',
  `stock_quantity` int NOT NULL DEFAULT 0 COMMENT '库存数量',
  `warning_stock` int NOT NULL DEFAULT 10 COMMENT '预警库存',
  `sales_count` int NOT NULL DEFAULT 0 COMMENT '销量',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `favorite_count` int NOT NULL DEFAULT 0 COMMENT '收藏量',
  `weight` decimal(8, 2) NULL DEFAULT NULL COMMENT '重量（kg）',
  `dimensions` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '尺寸（长x宽x高）',
  `warranty_period` int NOT NULL DEFAULT 12 COMMENT '保修期（月）',
  `shipping_fee` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '运费',
  `is_free_shipping` int NOT NULL DEFAULT 0 COMMENT '是否包邮：0-否，1-是',
  `status` int NOT NULL DEFAULT 1 COMMENT '商品状态：1-上架，2-下架，3-缺货',
  `is_hot` int NOT NULL DEFAULT 0 COMMENT '是否热销：0-否，1-是',
  `is_new` int NOT NULL DEFAULT 0 COMMENT '是否新品：0-否，1-是',
  `is_recommended` int NOT NULL DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_product_no`(`product_no` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_brand`(`brand` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_is_hot`(`is_hot` ASC) USING BTREE,
  INDEX `idx_is_new`(`is_new` ASC) USING BTREE,
  INDEX `idx_is_recommended`(`is_recommended` ASC) USING BTREE,
  INDEX `idx_selling_price`(`selling_price` ASC) USING BTREE,
  INDEX `idx_sales_count`(`sales_count` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_store_id`(`store_id` ASC) USING BTREE,
  INDEX `idx_fulfillment_type`(`fulfillment_type` ASC) USING BTREE,
  INDEX `idx_audit_status`(`audit_status` ASC) USING BTREE,
  INDEX `idx_is_frozen`(`is_frozen` ASC) USING BTREE,
  CONSTRAINT `fk_products_category_id` FOREIGN KEY (`category_id`) REFERENCES `product_categories` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for repair_order_faults
-- ----------------------------
DROP TABLE IF EXISTS `repair_order_faults`;
CREATE TABLE `repair_order_faults`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，ROF+雪花ID',
  `repair_order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '维修订单ID',
  `fault_phenomenon_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '故障现象ID',
  `fault_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '故障描述',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_repair_order_id`(`repair_order_id` ASC) USING BTREE,
  INDEX `idx_fault_phenomenon_id`(`fault_phenomenon_id` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_repair_order_faults_fault_phenomenon_id` FOREIGN KEY (`fault_phenomenon_id`) REFERENCES `fault_phenomena` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_repair_order_faults_repair_order_id` FOREIGN KEY (`repair_order_id`) REFERENCES `repair_orders` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维修订单故障记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for repair_order_payments
-- ----------------------------
DROP TABLE IF EXISTS `repair_order_payments`;
CREATE TABLE `repair_order_payments`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，ROP+雪花ID',
  `repair_order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '维修订单ID',
  `door_fee` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '上门费',
  `distance_fee` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '距离费',
  `service_distance_km` decimal(10, 3) NOT NULL DEFAULT 0.000 COMMENT '本次路线距离（公里）',
  `base_radius_km_snapshot` decimal(10, 3) NOT NULL DEFAULT 0.000 COMMENT '基础服务半径快照（公里）',
  `distance_over_km` decimal(10, 3) NOT NULL DEFAULT 0.000 COMMENT '超出基础半径公里数',
  `min_visit_fee_snapshot` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '最低上门费快照',
  `extra_fee_per_km_snapshot` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '超区每公里单价快照',
  `distance_calc_type_snapshot` int NOT NULL DEFAULT 1 COMMENT '距离计算方式快照：1-驾车，2-骑行，3-直线',
  `rounding_rule_snapshot` int NOT NULL DEFAULT 1 COMMENT '公里取整规则快照：1-向上取整，2-四舍五入，3-不取整',
  `pricing_locked_time` bigint NULL DEFAULT NULL COMMENT '价格锁定时间戳',
  `fee_rule_snapshot` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '计费规则快照JSON',
  `service_fee` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '服务费',
  `material_fee` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '材料费',
  `overtime_fee` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '超时费',
  `total_amount` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `actual_amount` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '实际支付金额',
  `discount_amount` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `coupon_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的优惠券ID',
  `payment_method` int NULL DEFAULT NULL COMMENT '支付方式：1-微信支付，2-支付宝，3-现金，4-银行卡',
  `payment_time` bigint NULL DEFAULT NULL COMMENT '支付时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_repair_order_id`(`repair_order_id` ASC) USING BTREE,
  INDEX `idx_payment_method`(`payment_method` ASC) USING BTREE,
  INDEX `idx_payment_time`(`payment_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_repair_order_payments_repair_order_id` FOREIGN KEY (`repair_order_id`) REFERENCES `repair_orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维修订单支付表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for repair_orders
-- ----------------------------
DROP TABLE IF EXISTS `repair_orders`;
CREATE TABLE `repair_orders`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，RO+雪花ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID',
  `service_type_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务类型ID',
  `appliance_brand` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电器品牌',
  `appliance_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电器型号',
  `purchase_date` bigint NULL DEFAULT NULL COMMENT '购买日期',
  `service_address_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '服务地址ID',
  `appointment_time` bigint NULL DEFAULT NULL COMMENT '预约时间戳',
  `status` int NOT NULL DEFAULT 1 COMMENT '订单状态：1-待接单，2-待上门，3-待检查，4-待支付，5-服务中，6-已完成，7-已取消，8-已退款',
  `payment_status` int NOT NULL DEFAULT 1 COMMENT '支付状态：1-待支付，2-已支付，3-已退款',
  `start_time` bigint NULL DEFAULT NULL COMMENT '开始服务时间戳',
  `end_time` bigint NULL DEFAULT NULL COMMENT '结束服务时间戳',
  `completion_time` bigint NULL DEFAULT NULL COMMENT '完成时间戳',
  `cancel_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '取消原因',
  `cancel_time` bigint NULL DEFAULT NULL COMMENT '取消时间戳',
  `cancel_reason_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联取消原因ID',
  `refund_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '退款原因',
  `refund_amount` decimal(8, 2) NULL DEFAULT 0.00 COMMENT '退款金额',
  `refund_time` bigint NULL DEFAULT NULL COMMENT '退款时间戳',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_technician_account_id`(`technician_account_id` ASC) USING BTREE,
  INDEX `idx_service_type_id`(`service_type_id` ASC) USING BTREE,
  INDEX `idx_service_address_id`(`service_address_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_payment_status`(`payment_status` ASC) USING BTREE,
  INDEX `idx_appointment_time`(`appointment_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_repair_orders_composite`(`status` ASC, `payment_status` ASC, `created_time` ASC) USING BTREE,
  CONSTRAINT `fk_repair_orders_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_repair_orders_service_address_id` FOREIGN KEY (`service_address_id`) REFERENCES `user_addresses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_repair_orders_service_type_id` FOREIGN KEY (`service_type_id`) REFERENCES `service_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_repair_orders_technician_account_id` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维修订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for reports
-- ----------------------------
DROP TABLE IF EXISTS `reports`;
CREATE TABLE `reports`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，RP+雪花ID',
  `reporter_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报人账号ID',
  `reporter_type` int NOT NULL COMMENT '举报人类型：1-用户，2-师傅，3-门店管理员',
  `target_type` int NOT NULL COMMENT '举报对象类型：1-账号，2-门店，3-订单，4-商品',
  `target_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报对象ID',
  `target_field` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '举报字段或节点',
  `reason_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报原因分类',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '补充说明',
  `evidence_images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '截图JSON数组',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-待处理，2-处理中，3-已成立，4-已驳回',
  `result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理结果说明',
  `handler_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理人ID',
  `handle_time` bigint NULL DEFAULT NULL COMMENT '处理时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_reporter_target_pending`(`reporter_id` ASC, `target_type` ASC, `target_id` ASC, `status` ASC) USING BTREE COMMENT '同一人对同一对象未处理的举报唯一',
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_reporter`(`reporter_id` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '举报表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for reviews
-- ----------------------------
DROP TABLE IF EXISTS `reviews`;
CREATE TABLE `reviews`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，R+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单ID',
  `order_type` int NOT NULL COMMENT '订单类型：1-维修订单，2-商品订单',
  `target_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '评价对象ID（师傅ID或商品ID）',
  `target_type` int NOT NULL COMMENT '评价对象类型：1-师傅，2-商品',
  `rating` int NOT NULL COMMENT '评分：1-5星',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评价内容',
  `is_anonymous` int NOT NULL DEFAULT 0 COMMENT '是否匿名：0-否，1-是',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-已隐藏',
  `reply_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '回复内容',
  `reply_time` bigint NULL DEFAULT NULL COMMENT '回复时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_id` ASC, `target_type` ASC) USING BTREE,
  INDEX `idx_rating`(`rating` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评价表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for service_categories
-- ----------------------------
DROP TABLE IF EXISTS `service_categories`;
CREATE TABLE `service_categories`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，SC+雪花ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类编码（唯一标识）',
  `parent_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '父分类ID，NULL表示顶级分类',
  `level` int NOT NULL DEFAULT 1 COMMENT '分类层级：1-一级分类，2-二级分类，3-三级分类',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类路径，如：/1/2/3/',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '分类描述',
  `is_active` int NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序权重',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  UNIQUE INDEX `uk_name_parent`(`name` ASC, `parent_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_path`(`path` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_service_categories_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `service_categories` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务类型分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of service_categories
-- ----------------------------
INSERT INTO `service_categories` VALUES ('SC900000000000000001', '家电维修', 'TEST_REPAIR', NULL, 1, '/SC900000000000000001/', '测试用一级分类：家电维修', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000002', '厨房电器', 'TEST_REPAIR_KITCHEN', 'SC900000000000000001', 2, '/SC900000000000000001/SC900000000000000002/', '测试用二级分类：厨房电器', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000003', '冰箱维修', 'TEST_REPAIR_FRIDGE', 'SC900000000000000002', 3, '/SC900000000000000001/SC900000000000000002/SC900000000000000003/', '测试用三级分类：冰箱维修', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000004', '油烟机维修', 'TEST_REPAIR_HOOD', 'SC900000000000000002', 3, '/SC900000000000000001/SC900000000000000002/SC900000000000000004/', '测试用三级分类：油烟机维修', 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000005', '清洁电器', 'TEST_REPAIR_CLEAN', 'SC900000000000000001', 2, '/SC900000000000000001/SC900000000000000005/', '测试用二级分类：清洁电器', 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000006', '洗衣机维修', 'TEST_REPAIR_WASHER', 'SC900000000000000005', 3, '/SC900000000000000001/SC900000000000000005/SC900000000000000006/', '测试用三级分类：洗衣机维修', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000007', '家电安装', 'TEST_INSTALL', NULL, 1, '/SC900000000000000007/', '测试用一级分类：家电安装', 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000008', '大家电安装', 'TEST_INSTALL_LARGE', 'SC900000000000000007', 2, '/SC900000000000000007/SC900000000000000008/', '测试用二级分类：大家电安装', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000009', '空调安装', 'TEST_INSTALL_AC', 'SC900000000000000008', 3, '/SC900000000000000007/SC900000000000000008/SC900000000000000009/', '测试用三级分类：空调安装', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000010', '厨房安装', 'TEST_INSTALL_KITCHEN', 'SC900000000000000007', 2, '/SC900000000000000007/SC900000000000000010/', '测试用二级分类：厨房安装', 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000011', '油烟机安装', 'TEST_INSTALL_HOOD', 'SC900000000000000010', 3, '/SC900000000000000007/SC900000000000000010/SC900000000000000011/', '测试用三级分类：油烟机安装', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000012', '到店维修', 'TEST_OFFLINE', NULL, 1, '/SC900000000000000012/', '测试用一级分类：到店维修', 1, 3, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000013', '小家电维修', 'TEST_OFFLINE_SMALL', 'SC900000000000000012', 2, '/SC900000000000000012/SC900000000000000013/', '测试用二级分类：小家电维修', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000014', '电饭煲维修', 'TEST_OFFLINE_COOKER', 'SC900000000000000013', 3, '/SC900000000000000012/SC900000000000000013/SC900000000000000014/', '测试用三级分类：电饭煲维修', 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_categories` VALUES ('SC900000000000000015', '微波炉维修', 'TEST_OFFLINE_MICROWAVE', 'SC900000000000000013', 3, '/SC900000000000000012/SC900000000000000013/SC900000000000000015/', '测试用三级分类：微波炉维修', 1, 2, 1774741181384, 1774741181384, 1, 0);

-- ----------------------------
-- Table structure for service_types
-- ----------------------------
DROP TABLE IF EXISTS `service_types`;
CREATE TABLE `service_types`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，ST+雪花ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务类型名称',
  `type` int NOT NULL DEFAULT 1 COMMENT '类型：1-上门维修，2-上门安装，3-线下维修',
  `category_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '服务分类ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '服务描述',
  `base_price` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '基础价格',
  `is_active` int NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name_category_type`(`name` ASC, `category_id` ASC, `type` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_service_types_category_id` FOREIGN KEY (`category_id`) REFERENCES `service_categories` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of service_types
-- ----------------------------
INSERT INTO `service_types` VALUES ('ST900000000000000001', '冰箱上门检修', 1, 'SC900000000000000003', '适用于冰箱常见故障的上门检测与维修', 60.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_types` VALUES ('ST900000000000000002', '油烟机上门检修', 1, 'SC900000000000000004', '适用于油烟机常见故障的上门检测与维修', 50.00, 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_types` VALUES ('ST900000000000000003', '洗衣机上门检修', 1, 'SC900000000000000006', '适用于洗衣机常见故障的上门检测与维修', 55.00, 1, 3, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_types` VALUES ('ST900000000000000004', '空调上门安装', 2, 'SC900000000000000009', '适用于家用空调的新机安装或移机安装', 120.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_types` VALUES ('ST900000000000000005', '油烟机上门安装', 2, 'SC900000000000000011', '适用于油烟机的新机安装或拆旧换新安装', 100.00, 1, 2, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_types` VALUES ('ST900000000000000006', '电饭煲到店检修', 3, 'SC900000000000000014', '适用于电饭煲送店后的检测与维修', 30.00, 1, 1, 1774741181384, 1774741181384, 1, 0);
INSERT INTO `service_types` VALUES ('ST900000000000000007', '微波炉到店检修', 3, 'SC900000000000000015', '适用于微波炉送店后的检测与维修', 35.00, 1, 2, 1774741181384, 1774741181384, 1, 0);

-- ----------------------------
-- Table structure for shopping_carts
-- ----------------------------
DROP TABLE IF EXISTS `shopping_carts`;
CREATE TABLE `shopping_carts`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，SC+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `product_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `selected` int NOT NULL DEFAULT 1 COMMENT '是否选中：0-否，1-是',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account_product`(`account_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_selected`(`selected` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  CONSTRAINT `fk_shopping_carts_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_shopping_carts_product_id` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '购物车表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for store_business_hours
-- ----------------------------
DROP TABLE IF EXISTS `store_business_hours`;
CREATE TABLE `store_business_hours`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，SBH+雪花ID',
  `store_id` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `day_of_week` int NOT NULL COMMENT '星期：1-7（周一到周日）',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `is_available` int NOT NULL DEFAULT 1 COMMENT '是否营业：0-休息，1-营业',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_store_day`(`store_id` ASC, `day_of_week` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_store_hours_store_id` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '门店营业时间表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for stores
-- ----------------------------
DROP TABLE IF EXISTS `stores`;
CREATE TABLE `stores`  (
  `id` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '门店名称',
  `logo_image_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '门店Logo图片ID',
  `store_admin_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '门店管理员ID',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '门店地址',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度',
  `business_status` int NOT NULL DEFAULT 1 COMMENT '营业状态：1-营业中，2-休息中，3-已关闭',
  `rating` decimal(3, 1) NULL DEFAULT NULL COMMENT '门店评分，1.0-5.0，初始NULL（暂无评分）',
  `rating_count` int NOT NULL DEFAULT 0 COMMENT '有效评价数，≥3后开始展示评分',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '门店介绍',
  `business_license` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '营业执照URL',
  `audit_status` int NOT NULL DEFAULT 1 COMMENT '审核状态：1-待审核，2-审核通过，3-审核拒绝',
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核备注',
  `audit_time` bigint NULL DEFAULT NULL COMMENT '审核时间戳',
  `is_online` int NOT NULL DEFAULT 1 COMMENT '是否在线：0-离线，1-在线',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_admin_id`(`store_admin_id` ASC) USING BTREE,
  INDEX `idx_business_status`(`business_status` ASC) USING BTREE,
  INDEX `idx_audit_status`(`audit_status` ASC) USING BTREE,
  INDEX `idx_is_online`(`is_online` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_location`(`latitude` ASC, `longitude` ASC) USING BTREE,
  CONSTRAINT `fk_stores_store_admin_id` FOREIGN KEY (`store_admin_id`) REFERENCES `admin_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '门店表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for system_configs
-- ----------------------------
DROP TABLE IF EXISTS `system_configs`;
CREATE TABLE `system_configs`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，SC+雪花ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置值',
  `config_type` int NOT NULL DEFAULT 1 COMMENT '配置类型：1-字符串，2-数字，3-布尔值，4-JSON',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '配置描述',
  `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置分组',
  `is_system` int NOT NULL DEFAULT 0 COMMENT '是否系统配置：0-否，1-是',
  `is_encrypted` int NOT NULL DEFAULT 0 COMMENT '是否加密：0-否，1-是',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE,
  INDEX `idx_group_name`(`group_name` ASC) USING BTREE,
  INDEX `idx_is_system`(`is_system` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of system_configs
-- ----------------------------
INSERT INTO `system_configs` VALUES ('SC296281291291234304', 'after_sales.valid_days', '7', 2, '订单完成后允许申请售后的天数，同时用于售后保护期资金释放。', 'after_sales', 1, 0, 1774706164484, 1781673467012, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291299622913', 'after_sales.max_image_count', '5', 2, '用户提交售后时最多可上传的图片数量。', 'after_sales', 1, 0, 1774706164484, 1781673467023, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291303817218', 'account.cancel_grace_days', '7', 2, '用户或师傅提交注销申请后，可撤销注销的保留天数。', 'account', 1, 0, 1774706164484, 1781673467034, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291308011523', 'account.cancel_data_retention_days', '30', 2, '账号注销记录保留时长。', 'account', 1, 0, 1774706164484, 1781673467043, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291308011524', 'auth.code_expire_minutes', '5', 2, '邮箱验证码在 Redis 中的有效分钟数，同时同步展示到邮件模板。', 'auth', 1, 0, 1774706164484, 1781673467054, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291308011525', 'order.appointment.default_days', '7', 2, '前端默认展示的预约日期范围。', 'order', 1, 0, 1774706164484, 1781673467062, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291312205830', 'order.appointment.max_days', '30', 2, '用户最多可预约到未来多少天。', 'order', 1, 0, 1774706164484, 1781673467072, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291316400135', 'order.appointment.min_lead_minutes', '60', 2, '用户选择预约时间时，必须至少提前的分钟数。', 'order', 1, 0, 1774706164484, 1781673467097, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291320594440', 'door_qr.fallback_expire_hours', '24', 2, '无预约时间时，上门二维码默认有效小时数。', 'door_qr', 1, 0, 1774706164484, 1781673467108, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291320594441', 'door_qr.after_appointment_expire_hours', '2', 2, '二维码在预约时间之后继续保持有效的小时数。', 'door_qr', 1, 0, 1774706164484, 1781673467117, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296281291324788746', 'door_qr.min_valid_hours', '2', 2, '二维码生成后，从当前时间起至少保留有效的小时数。', 'door_qr', 1, 0, 1774706164484, 1781673467126, 4, 0);
INSERT INTO `system_configs` VALUES ('SC296285663802822656', 'protocol.user_agreement_file_id', 'FI325504240929869824', 1, '用户协议当前启用的文件ID', 'protocol', 1, 0, 1774707206972, 1781673458626, 2, 0);
INSERT INTO `system_configs` VALUES ('SC296285676117299200', 'protocol.privacy_policy_file_id', 'FI325504226367246336', 1, '隐私协议当前启用的文件ID', 'protocol', 1, 0, 1774707209908, 1781673455151, 2, 0);
INSERT INTO `system_configs` VALUES ('SC296294599893323776', 'email_template.auth_code_file_id', 'FI325504191906844672', 1, '验证码邮件模板当前启用的文件ID', 'email_template', 1, 0, 1774709337500, 1781673446938, 3, 0);
INSERT INTO `system_configs` VALUES ('SC325502845040332800', 'credit.recovery.no_violation_days', '30', 2, '连续多少天无新增违规后自动恢复积分。', 'credit', 1, 0, 1781673125807, 1781673467203, 2, 0);
INSERT INTO `system_configs` VALUES ('SC325502845082275840', 'credit.recovery.no_violation_score', '5', 2, '无违规达到天数后每次恢复的积分。', 'credit', 1, 0, 1781673125818, 1781673467213, 2, 0);
INSERT INTO `system_configs` VALUES ('SC325502845099053056', 'credit.recovery.report_bonus', '2', 2, '有效举报成立每次奖励的积分。', 'credit', 1, 0, 1781673125822, 1781673467221, 2, 0);
INSERT INTO `system_configs` VALUES ('SC325502845115830272', 'credit.recovery.report_monthly_max', '10', 2, '举报奖励每月最多可获得积分。', 'credit', 1, 0, 1781673125826, 1781673467227, 2, 0);
INSERT INTO `system_configs` VALUES ('SC325502845136801792', 'credit.recovery.order_monthly_max', '10', 2, '正常完成订单每月最多恢复积分。', 'credit', 1, 0, 1781673125831, 1781673467234, 2, 0);
INSERT INTO `system_configs` VALUES ('SC325502845153579008', 'ban.default_hours', '168', 2, '执行封禁时默认的封禁小时数。', 'ban', 1, 0, 1781673125835, 1781673467247, 2, 0);
INSERT INTO `system_configs` VALUES ('SC900000000000000016', 'credit.score_initial', '100', 2, '用户和师傅注册时的默认信用积分。', 'credit', 1, 0, 1779944058000, 1781673467134, 2, 0);
INSERT INTO `system_configs` VALUES ('SC900000000000000017', 'credit.score_redline', '40', 2, '信用积分低于此值时触发封禁流程。', 'credit', 1, 0, 1779944058000, 1781673467142, 2, 0);
INSERT INTO `system_configs` VALUES ('SC900000000000000018', 'credit.score_warning', '60', 2, '信用积分低于此值时限制部分核心功能。', 'credit', 1, 0, 1779944058000, 1781673467149, 2, 0);
INSERT INTO `system_configs` VALUES ('SC900000000000000019', 'credit.deduct_level1', '5', 2, '轻微违规扣除的信用积分。', 'penalty', 1, 0, 1779944058000, 1781673467158, 2, 0);
INSERT INTO `system_configs` VALUES ('SC900000000000000020', 'credit.deduct_level2', '10', 2, '一般违规扣除的信用积分。', 'penalty', 1, 0, 1779944058000, 1781673467169, 2, 0);
INSERT INTO `system_configs` VALUES ('SC900000000000000021', 'credit.deduct_level3', '20', 2, '严重违规扣除的信用积分。', 'penalty', 1, 0, 1779944058000, 1781673467178, 2, 0);
INSERT INTO `system_configs` VALUES ('SC900000000000000022', 'credit.deduct_level4', '40', 2, '重大违规扣除的信用积分。', 'penalty', 1, 0, 1779944058000, 1781673467192, 2, 0);
INSERT INTO `system_configs` VALUES ('SC900000000000000028', 'report.daily_max_count', '10', 2, '每个账号每天最多提交的举报次数。', 'report', 1, 0, 1779944058000, 1781673467240, 2, 0);

-- ----------------------------
-- Table structure for system_messages
-- ----------------------------
DROP TABLE IF EXISTS `system_messages`;
CREATE TABLE `system_messages`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，SM+雪花ID',
  `receiver_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接收人账号ID',
  `receiver_type` int NOT NULL COMMENT '接收人类型：1-用户，2-师傅，3-管理员',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `message_type` int NOT NULL DEFAULT 1 COMMENT '消息类型：1-系统通知，2-订单通知，3-账户通知，4-其他',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型编码',
  `business_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务ID',
  `priority` int NOT NULL DEFAULT 2 COMMENT '优先级：1-高，2-中，3-低',
  `is_read` int NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `read_time` bigint NULL DEFAULT NULL COMMENT '已读时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳（发送时间）',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_receiver`(`receiver_id` ASC, `receiver_type` ASC, `is_read` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_message_type`(`message_type` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统消息表（站内通知）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for technician_accounts
-- ----------------------------
DROP TABLE IF EXISTS `technician_accounts`;
CREATE TABLE `technician_accounts`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TA+雪花ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `wechat_openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信OpenID',
  `wechat_unionid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信UnionID',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码哈希',
  `salt` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码盐值',
  `account_status` int NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，2-未认证，3-冻结，4-离职',
  `token_version` int NOT NULL DEFAULT 1 COMMENT '令牌版本号',
  `last_login_time` bigint NULL DEFAULT NULL COMMENT '最后登录时间戳',
  `last_login_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `work_status` int NOT NULL DEFAULT 1 COMMENT '工作状态：0-离线，1-在线，2-忙碌，3-休息',
  `rating` decimal(3, 2) NOT NULL DEFAULT 5.00 COMMENT '评分（1-5分）',
  `order_count` int NOT NULL DEFAULT 0 COMMENT '完成订单数',
  `completion_rate` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '完成率',
  `store_id` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `credit_score` int NOT NULL DEFAULT 100 COMMENT '信用积分，默认100',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
  UNIQUE INDEX `uk_wechat_openid`(`wechat_openid` ASC) USING BTREE,
  INDEX `idx_wechat_unionid`(`wechat_unionid` ASC) USING BTREE,
  INDEX `idx_account_status`(`account_status` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_work_status`(`work_status` ASC) USING BTREE,
  INDEX `idx_rating`(`rating` ASC) USING BTREE,
  INDEX `idx_store_id`(`store_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '师傅账号表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for technician_bindings
-- ----------------------------
DROP TABLE IF EXISTS `technician_bindings`;
CREATE TABLE `technician_bindings`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键，TB+雪花ID',
  `store_id` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `technician_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '师傅ID',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-待确认，2-已绑定，3-申请解绑，4-已解绑',
  `invited_time` bigint NOT NULL COMMENT '邀请时间戳',
  `confirmed_time` bigint NULL DEFAULT NULL COMMENT '确认时间戳',
  `unbind_requested_time` bigint NULL DEFAULT NULL COMMENT '解绑申请时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id`(`store_id` ASC) USING BTREE,
  INDEX `idx_technician_id`(`technician_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '师傅绑定记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for technician_profiles
-- ----------------------------
DROP TABLE IF EXISTS `technician_profiles`;
CREATE TABLE `technician_profiles`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TP+雪花ID',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证号',
  `gender` int NULL DEFAULT NULL COMMENT '性别：1-男，2-女',
  `birthday` bigint NULL DEFAULT NULL COMMENT '生日',
  `work_years` int NOT NULL DEFAULT 0 COMMENT '工作年限',
  `education` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历',
  `certificates` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '证书JSON数组',
  `specialties` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '专长JSON数组',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '个人介绍',
  `response_time` int NOT NULL DEFAULT 0 COMMENT '平均响应时间（分钟）',
  `location_update_time` bigint NULL DEFAULT NULL COMMENT '位置更新时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_technician_account_id`(`technician_account_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_id_card`(`id_card` ASC) USING BTREE,
  INDEX `idx_real_name`(`real_name` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_technician_profiles_technician_account_id` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '师傅信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for technician_service_areas
-- ----------------------------
DROP TABLE IF EXISTS `technician_service_areas`;
CREATE TABLE `technician_service_areas`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TSA+雪花ID',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID',
  `center_latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '服务中心点纬度',
  `center_longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '服务中心点经度',
  `center_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '服务中心点地址',
  `area_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '服务区域名称',
  `is_default` int NOT NULL DEFAULT 1 COMMENT '是否默认区域：0-否，1-是',
  `is_active` int NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_technician_area_name`(`technician_account_id` ASC, `area_name` ASC) USING BTREE,
  INDEX `idx_technician_account_id`(`technician_account_id` ASC) USING BTREE,
  INDEX `idx_center_location`(`center_latitude` ASC, `center_longitude` ASC) USING BTREE,
  INDEX `idx_is_default`(`is_default` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_technician_service_areas_technician_account_id` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '师傅服务区域中心表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for technician_skills
-- ----------------------------
DROP TABLE IF EXISTS `technician_skills`;
CREATE TABLE `technician_skills`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TS+雪花ID',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID',
  `service_type_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务类型ID',
  `skill_level` int NOT NULL DEFAULT 1 COMMENT '技能等级：1-初级，2-中级，3-高级，4-专家',
  `certification_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '认证证书URL',
  `is_active` int NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_technician_service`(`technician_account_id` ASC, `service_type_id` ASC) USING BTREE,
  INDEX `idx_service_type_id`(`service_type_id` ASC) USING BTREE,
  INDEX `idx_skill_level`(`skill_level` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_technician_skills_service_type_id` FOREIGN KEY (`service_type_id`) REFERENCES `service_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_technician_skills_technician_account_id` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '师傅技能表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for technician_visit_fee_policies
-- ----------------------------
DROP TABLE IF EXISTS `technician_visit_fee_policies`;
CREATE TABLE `technician_visit_fee_policies`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TVP+雪花ID',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID',
  `service_kind` int NOT NULL DEFAULT 1 COMMENT '上门服务类型：1-上门维修，2-上门安装',
  `min_visit_fee` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '最低上门费',
  `base_radius_km` decimal(10, 3) NOT NULL DEFAULT 0.000 COMMENT '基础服务半径（公里）',
  `extra_fee_per_km` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '超出基础半径每公里费用',
  `distance_calc_type` int NOT NULL DEFAULT 1 COMMENT '距离计算方式：1-驾车，2-骑行',
  `rounding_rule` int NOT NULL DEFAULT 1 COMMENT '公里取整规则：1-向上取整，2-四舍五入',
  `max_visit_fee` decimal(8, 2) NULL DEFAULT NULL COMMENT '上门费封顶公里数（可空）',
  `is_active` int NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `effective_time` bigint NOT NULL COMMENT '生效时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_technician_kind_effective`(`technician_account_id` ASC, `service_kind` ASC, `effective_time` ASC) USING BTREE,
  INDEX `idx_technician_account_id`(`technician_account_id` ASC) USING BTREE,
  INDEX `idx_service_kind`(`service_kind` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_effective_time`(`effective_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_technician_visit_fee_policies_technician_account_id` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '师傅上门计费策略表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for technician_work_times
-- ----------------------------
DROP TABLE IF EXISTS `technician_work_times`;
CREATE TABLE `technician_work_times`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，TW+雪花ID',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '师傅账号ID',
  `day_of_week` int NOT NULL COMMENT '星期几：1-7（周一到周日）',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `is_available` int NOT NULL DEFAULT 1 COMMENT '是否可用：0-不可用，1-可用',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_technician_day`(`technician_account_id` ASC, `day_of_week` ASC) USING BTREE,
  INDEX `idx_day_of_week`(`day_of_week` ASC) USING BTREE,
  INDEX `idx_is_available`(`is_available` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_technician_work_times_technician_account_id` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '师傅工作时间表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_accounts
-- ----------------------------
DROP TABLE IF EXISTS `user_accounts`;
CREATE TABLE `user_accounts`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，A+雪花ID',
  `wx_openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信OpenID',
  `wx_unionid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信UnionID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '昵称',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '哈希密码',
  `salt` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码盐值',
  `is_verified` int NOT NULL DEFAULT 0 COMMENT '是否实名认证：0-未认证，1-已认证',
  `balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-冻结，3-注销申请中，4-已注销',
  `token_version` int NOT NULL DEFAULT 1 COMMENT '令牌版本号',
  `cancel_apply_time` bigint NULL DEFAULT NULL COMMENT '注销申请时间戳',
  `cancel_time` bigint NULL DEFAULT NULL COMMENT '注销时间戳',
  `credit_score` int NOT NULL DEFAULT 100 COMMENT '信用积分，默认100',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wx_openid`(`wx_openid` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_wx_unionid`(`wx_unionid` ASC) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_is_verified`(`is_verified` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户账号表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_addresses
-- ----------------------------
DROP TABLE IF EXISTS `user_addresses`;
CREATE TABLE `user_addresses`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，UA+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号ID',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系人电话',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '省份',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '城市',
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '区县',
  `street` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '街道',
  `detailed_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '详细地址',
  `postal_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮政编码',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度',
  `is_default` int NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
  `address_type` int NOT NULL DEFAULT 1 COMMENT '地址类型：1-家庭，2-公司，3-其他',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_is_default`(`is_default` ASC) USING BTREE,
  INDEX `idx_address_type`(`address_type` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_user_addresses_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户地址表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_coupons
-- ----------------------------
DROP TABLE IF EXISTS `user_coupons`;
CREATE TABLE `user_coupons`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，UC+雪花ID',
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户ID',
  `coupon_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '优惠券ID',
  `receive_time` bigint NOT NULL COMMENT '领取时间戳',
  `use_time` bigint NULL DEFAULT NULL COMMENT '使用时间戳',
  `expire_time` bigint NOT NULL COMMENT '过期时间戳',
  `order_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的订单ID',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-未使用，2-已使用，3-已过期',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_coupon`(`user_id` ASC, `coupon_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_coupon_id`(`coupon_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_expire_time`(`expire_time` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_user_coupons_coupon_id` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_coupons_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户优惠券表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_follow_technicians
-- ----------------------------
DROP TABLE IF EXISTS `user_follow_technicians`;
CREATE TABLE `user_follow_technicians`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，UF+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号ID',
  `technician_account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '维修师傅账号ID',
  `created_time` bigint NOT NULL COMMENT '创建时间戳（毫秒）',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳（毫秒）',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_technician`(`account_id` ASC, `technician_account_id` ASC) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_technician_account_id`(`technician_account_id` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_user_follow_technicians_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_follow_technicians_technician_account_id` FOREIGN KEY (`technician_account_id`) REFERENCES `technician_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户关注师傅关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_profiles
-- ----------------------------
DROP TABLE IF EXISTS `user_profiles`;
CREATE TABLE `user_profiles`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，UP+雪花ID',
  `account_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号ID',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `gender` int NULL DEFAULT NULL COMMENT '性别：1-男，2-女，3-未知',
  `birthday` bigint NULL DEFAULT NULL COMMENT '生日',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证号',
  `profession` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职业',
  `company` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '公司',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '个人简介',
  `emergency_contact` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '紧急联系人电话',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account_id`(`account_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_id_card`(`id_card` ASC) USING BTREE,
  INDEX `idx_real_name`(`real_name` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_user_profiles_account_id` FOREIGN KEY (`account_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for videos
-- ----------------------------
DROP TABLE IF EXISTS `videos`;
CREATE TABLE `videos`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，VI+雪花ID',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件路径',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问URL',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MIME类型',
  `duration` int NULL DEFAULT NULL COMMENT '视频时长（秒）',
  `width` int NULL DEFAULT NULL COMMENT '视频宽度',
  `height` int NULL DEFAULT NULL COMMENT '视频高度',
  `thumbnail_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '缩略图URL',
  `uploader_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上传者ID',
  `uploader_type` int NOT NULL COMMENT '上传者类型：1-用户，2-师傅，3-管理员',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务ID',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uploader`(`uploader_id` ASC, `uploader_type` ASC) USING BTREE,
  INDEX `idx_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '视频表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for warranty_card_usage_records
-- ----------------------------
DROP TABLE IF EXISTS `warranty_card_usage_records`;
CREATE TABLE `warranty_card_usage_records`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，WU+雪花ID',
  `warranty_card_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '保修卡ID',
  `card_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '保修卡号',
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户ID',
  `product_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品ID',
  `product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `product_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品型号',
  `issue_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '故障描述',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `status` int NOT NULL DEFAULT 1 COMMENT '申请状态：1-待处理，2-已完成，3-已驳回',
  `process_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理备注',
  `apply_time` bigint NOT NULL COMMENT '申请时间戳',
  `process_time` bigint NULL DEFAULT NULL COMMENT '处理时间戳',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_warranty_card_id`(`warranty_card_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_warranty_usage_card_id` FOREIGN KEY (`warranty_card_id`) REFERENCES `warranty_cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_warranty_usage_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '保修卡使用申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for warranty_cards
-- ----------------------------
DROP TABLE IF EXISTS `warranty_cards`;
CREATE TABLE `warranty_cards`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键，WC+雪花ID',
  `card_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '保修卡号',
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户ID',
  `product_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品ID',
  `product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `product_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品型号',
  `purchase_date` date NOT NULL COMMENT '购买日期',
  `warranty_start_date` date NOT NULL COMMENT '保修开始日期',
  `warranty_end_date` date NOT NULL COMMENT '保修结束日期',
  `warranty_period` int NOT NULL COMMENT '保修期（月）',
  `warranty_type` int NOT NULL DEFAULT 1 COMMENT '保修类型：1-厂家保修，2-店铺保修，3-延保',
  `warranty_status` int NOT NULL DEFAULT 1 COMMENT '保修状态：1-有效，2-已过期，3-已使用',
  `repair_count` int NOT NULL DEFAULT 0 COMMENT '维修次数',
  `last_repair_date` date NULL DEFAULT NULL COMMENT '最后维修日期',
  `created_time` bigint NOT NULL COMMENT '创建时间戳',
  `updated_time` bigint NOT NULL COMMENT '更新时间戳',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_card_no`(`card_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_warranty_status`(`warranty_status` ASC) USING BTREE,
  INDEX `idx_warranty_end_date`(`warranty_end_date` ASC) USING BTREE,
  INDEX `idx_is_delete`(`is_delete` ASC) USING BTREE,
  CONSTRAINT `fk_warranty_cards_product_id` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_warranty_cards_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_accounts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '保修卡表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
