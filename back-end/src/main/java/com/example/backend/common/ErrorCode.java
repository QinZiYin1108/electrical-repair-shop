package com.example.backend.common;

public enum ErrorCode {

    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    DUPLICATE_KEY(409, "数据已存在"),
    DATA_INTEGRITY_ERROR(422, "数据完整性错误"),
    BUSINESS_ERROR(500, "业务异常"),
    SQL_ERROR(500, "数据库异常"),
    SYSTEM_ERROR(500, "系统错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
