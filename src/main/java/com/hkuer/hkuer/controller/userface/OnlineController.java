package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.userface.OnlineService;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 在线状态相关接口
 */
@RestController
@RequestMapping("/online")
@RequiredArgsConstructor
public class OnlineController {

    private final OnlineService onlineService;

    /**
     * 获取当前在线人数
     */
    @GetMapping("/count")
    public Result getOnlineCount() {
        long count = onlineService.getOnlineCount();
        Map<String, Object> data = new HashMap<>();
        data.put("onlineCount", count);
        return Result.success(data);
    }

    /**
     * 心跳接口 - 前端可定期调用，维持在线状态
     * 注：如果使用 JwtAuthenticationFilter 自动心跳，此接口可选
     */
    @PostMapping("/heartbeat")
    public Result heartbeat() {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("401", "未登录");
        }
        onlineService.heartbeat(userId);
        return Result.success("心跳成功");
    }
}
