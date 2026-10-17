package com.hkuer.hkuer.exception;

/**
 * 内容安全风险异常
 * 当检测到内容包含违规信息时抛出
 */
public class ContentRiskException extends RuntimeException {
    
    private final String code;
    private final String msg;

    public ContentRiskException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public ContentRiskException(String msg) {
        this("400", msg);
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
