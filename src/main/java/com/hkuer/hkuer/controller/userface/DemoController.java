package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例控制器 - 演示如何获取当前用户ID
 */
@RestController
@RequestMapping("/demo")
@Slf4j
public class DemoController {

    @GetMapping("/current-user")
    public Result getCurrentUser() {
        String currentUserId = UserContext.getCurrentUserId();
        
        if (currentUserId == null) {
            return Result.error("401", "用户未登录");
        }
        
        log.info("当前用户ID: {}", currentUserId);
        return Result.success("当前用户ID: " + currentUserId);
    }
}
