package com.hkuer.hkuer.exception;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况（如禁言、权限不足等）
 */
public class BusinessException extends RuntimeException {
    
    private final String code;
    private final String msg;

    public BusinessException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(String msg) {
        this("400", msg);
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
