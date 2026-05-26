package com.example.backend.common.system;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SystemConfigRegistry {

    public static final int TYPE_STRING = 1;
    public static final int TYPE_NUMBER = 2;
    public static final int TYPE_BOOLEAN = 3;
    public static final int TYPE_JSON = 4;

    public static final String AFTER_SALES_VALID_DAYS = "after_sales.valid_days";
    public static final String AFTER_SALES_MAX_IMAGE_COUNT = "after_sales.max_image_count";
    public static final String ACCOUNT_CANCEL_GRACE_DAYS = "account.cancel_grace_days";
    public static final String ACCOUNT_CANCEL_DATA_RETENTION_DAYS = "account.cancel_data_retention_days";
    public static final String AUTH_CODE_EXPIRE_MINUTES = "auth.code_expire_minutes";
    public static final String ORDER_APPOINTMENT_DEFAULT_DAYS = "order.appointment.default_days";
    public static final String ORDER_APPOINTMENT_MAX_DAYS = "order.appointment.max_days";
    public static final String ORDER_APPOINTMENT_MIN_LEAD_MINUTES = "order.appointment.min_lead_minutes";
    public static final String DOOR_QR_FALLBACK_EXPIRE_HOURS = "door_qr.fallback_expire_hours";
    public static final String DOOR_QR_AFTER_APPOINTMENT_EXPIRE_HOURS = "door_qr.after_appointment_expire_hours";
    public static final String DOOR_QR_MIN_VALID_HOURS = "door_qr.min_valid_hours";

    private static final List<SystemConfigDefinition> DEFINITIONS = List.of(
        new SystemConfigDefinition(
            AFTER_SALES_VALID_DAYS,
            "after_sales",
            "售后设置",
            "售后申请有效天数",
            "订单完成后允许申请售后的天数，同时用于售后保护期资金释放。",
            TYPE_NUMBER,
            "7",
            "天",
            1L,
            365L,
            10
        ),
        new SystemConfigDefinition(
            AFTER_SALES_MAX_IMAGE_COUNT,
            "after_sales",
            "售后设置",
            "售后图片上限",
            "用户提交售后时最多可上传的图片数量。",
            TYPE_NUMBER,
            "5",
            "张",
            1L,
            20L,
            20
        ),
        new SystemConfigDefinition(
            ACCOUNT_CANCEL_GRACE_DAYS,
            "account",
            "账号设置",
            "注销反悔期",
            "用户或师傅提交注销申请后，可撤销注销的保留天数。",
            TYPE_NUMBER,
            "7",
            "天",
            1L,
            365L,
            10
        ),
        new SystemConfigDefinition(
            ACCOUNT_CANCEL_DATA_RETENTION_DAYS,
            "account",
            "账号设置",
            "注销记录保留天数",
            "账号注销记录保留时长。",
            TYPE_NUMBER,
            "30",
            "天",
            1L,
            3650L,
            20
        ),
        new SystemConfigDefinition(
            AUTH_CODE_EXPIRE_MINUTES,
            "auth",
            "验证码设置",
            "验证码有效期",
            "邮箱验证码在 Redis 中的有效分钟数，同时同步展示到邮件模板。",
            TYPE_NUMBER,
            "5",
            "分钟",
            1L,
            60L,
            10
        ),
        new SystemConfigDefinition(
            ORDER_APPOINTMENT_DEFAULT_DAYS,
            "order",
            "预约设置",
            "默认预约天数",
            "前端默认展示的预约日期范围。",
            TYPE_NUMBER,
            "7",
            "天",
            1L,
            365L,
            10
        ),
        new SystemConfigDefinition(
            ORDER_APPOINTMENT_MAX_DAYS,
            "order",
            "预约设置",
            "最大预约天数",
            "用户最多可预约到未来多少天。",
            TYPE_NUMBER,
            "30",
            "天",
            1L,
            365L,
            20
        ),
        new SystemConfigDefinition(
            ORDER_APPOINTMENT_MIN_LEAD_MINUTES,
            "order",
            "预约设置",
            "最少提前预约时间",
            "用户选择预约时间时，必须至少提前的分钟数。",
            TYPE_NUMBER,
            "60",
            "分钟",
            0L,
            1440L,
            30
        ),
        new SystemConfigDefinition(
            DOOR_QR_FALLBACK_EXPIRE_HOURS,
            "door_qr",
            "上门二维码设置",
            "兜底有效期",
            "无预约时间时，上门二维码默认有效小时数。",
            TYPE_NUMBER,
            "24",
            "小时",
            1L,
            720L,
            10
        ),
        new SystemConfigDefinition(
            DOOR_QR_AFTER_APPOINTMENT_EXPIRE_HOURS,
            "door_qr",
            "上门二维码设置",
            "预约后延长有效期",
            "二维码在预约时间之后继续保持有效的小时数。",
            TYPE_NUMBER,
            "2",
            "小时",
            1L,
            168L,
            20
        ),
        new SystemConfigDefinition(
            DOOR_QR_MIN_VALID_HOURS,
            "door_qr",
            "上门二维码设置",
            "最少保留有效期",
            "二维码生成后，从当前时间起至少保留有效的小时数。",
            TYPE_NUMBER,
            "2",
            "小时",
            1L,
            168L,
            30
        )
    );

    private static final Map<String, SystemConfigDefinition> DEFINITION_MAP;

    static {
        Map<String, SystemConfigDefinition> map = new LinkedHashMap<>();
        for (SystemConfigDefinition definition : DEFINITIONS) {
            map.put(definition.getKey(), definition);
        }
        DEFINITION_MAP = Collections.unmodifiableMap(map);
    }

    private SystemConfigRegistry() {
    }

    public static List<SystemConfigDefinition> getDefinitions() {
        return DEFINITIONS;
    }

    public static SystemConfigDefinition getDefinition(String key) {
        return DEFINITION_MAP.get(key);
    }

    public static boolean contains(String key) {
        return DEFINITION_MAP.containsKey(key);
    }
}
