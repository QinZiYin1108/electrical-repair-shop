-- 优惠券表增加门店ID
ALTER TABLE coupons ADD COLUMN store_id VARCHAR(20) NULL DEFAULT NULL COMMENT '归属门店ID（平台优惠券为NULL）' AFTER is_delete;
