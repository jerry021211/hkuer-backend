package com.hkuer.hkuer.common;

/**
 * 统一后端返回的数据结果类型
 */
public class Result {
    // 请求状态码
    private String code;
    // 成功或错误信息
    private String msg;
    // 返回数据结果
    private Object data;

    // 常用静态方法

    // 请求成功且无数据返回
    public static Result success() {
        Result result = new Result();
        result.setCode("200");
        result.setMsg("请求成功");
        return result;
    }

    // 请求成功且有数据返回。
    public static Result success(Object data) {
        // 直接调用静态方法设置状态码、请求成功信息
        Result result = Result.success();
        result.setData(data);
        return result;
    }

    // 请求失败且无数据返回
    public static Result error() {
        Result result = new Result();
        result.setCode("500");
        result.setMsg("系统出错了！");
        return result;
    }

    // 请求失败且无数据返回。适配自定义异常（传递code、msg）
    public static Result error(String code, String msg) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    // 请求失败但有数据返回。用于业务场景（如code=3000但需要返回postId）
    public static Result error(String code, String msg, Object data) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    // getter、setter方法
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
