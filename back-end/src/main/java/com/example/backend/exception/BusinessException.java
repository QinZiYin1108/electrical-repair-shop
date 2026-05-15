package com.example.backend.exception;

import com.example.backend.common.ErrorCode;
import lombok.Getter;

public class BusinessException extends RuntimeException {

    @Getter
    private Integer code;
    private String message;

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
