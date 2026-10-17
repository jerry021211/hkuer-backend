package com.hkuer.hkuer.controller;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.exception.CustomerException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController

public class webTest {
    @GetMapping("/hello")
    public Result hello() {
        return Result.success("hello");
    }
    @GetMapping("/map")
    public Result map(){
        Map<String,Object> map = new HashMap();
        map.put("username","岁岁岁平安");
        map.put("age",18);
        return Result.success(map);
    }
    @GetMapping("/count")
    public Result count(){
        int i = 1 / 0;
        return Result.success(666);
    }
    @GetMapping("/count2")
    public Result count2() throws CustomerException {
        throw new CustomerException("400","错误！禁止访问该路径");
    }
}

