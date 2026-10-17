package com.hkuer.hkuer.exception;

import com.hkuer.hkuer.common.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice("com.hkuer.hkuer.controller")
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Result error(Exception e) {
        e.printStackTrace();
        return Result.error();
    }

    @ExceptionHandler(CustomerException.class)
    @ResponseBody
    public Result error(CustomerException e) {
        e.printStackTrace();
        return Result.error(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result error(BusinessException e) {
        return Result.error(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(ContentRiskException.class)
    @ResponseBody
    public Result error(ContentRiskException e) {
        return Result.error(e.getCode(), e.getMsg());
    }
}
