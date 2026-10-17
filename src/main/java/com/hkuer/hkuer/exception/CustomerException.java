package com.hkuer.hkuer.exception;

public class CustomerException extends Exception{
    private String code;
    private String msg;

    public CustomerException(String msg, String code) {
        this.msg = msg;
        this.code = code;
    }

    //getter|setter方法
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}