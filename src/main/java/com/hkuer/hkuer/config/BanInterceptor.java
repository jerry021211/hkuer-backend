package com.hkuer.hkuer.config;

import com.hkuer.hkuer.service.userface.BanService;
import com.hkuer.hkuer.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 封号拦截器
 * 
 * 作用：拦截所有需要登录的接口，检查用户是否被封号
 * 
 * 核心逻辑：
 * 1. 从 UserContext 获取当前用户ID（由 JwtAuthenticationFilter 在 Filter 层设置）
 * 2. 查询 Redis 黑名单（Key: user:banned:{userId}）
 * 3. 如果 Key 存在，说明用户被封号，返回 403 Forbidden
 * 4. 如果 Key 不存在，放行请求
 * 
 * HTTP 状态码语义：
 * - 401 Unauthorized: 身份无效（未登录/Token失效）→ 动作：去登录
 * - 403 Forbidden: 身份有效但被禁止（封号/无权限）→ 动作：踢下线
 * 
 * 注意：
 * - 此拦截器必须在 JwtAuthenticationFilter 之后执行
 * - 使用 Redis 检查而非数据库，保证高性能（O(1) 复杂度）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BanInterceptor implements HandlerInterceptor {

    private final BanService banService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        // 1. 从 UserContext 获取当前用户ID
        // UserContext 由 JwtAuthenticationFilter 在 Filter 层设置
        String userId = UserContext.getCurrentUserId();

        // 2. 如果没有用户ID（未登录或白名单路径），直接放行
        // 白名单路径的 Token 验证已由 JwtAuthenticationFilter 处理
        if (userId == null || userId.trim().isEmpty()) {
            return true;
        }

        // 3. 检查 Redis 黑名单
        // isBanned() 方法检查 Redis 中是否存在 Key: user:banned:{userId}
        // 这是一个 O(1) 操作，性能极高
        if (banService.isBanned(userId)) {
            // 用户被封号，返回 403 Forbidden
            // 403 含义：服务器理解请求，但拒绝执行（你有身份证，但你上了黑名单）
            log.warn("封号用户尝试访问接口，userId: {}, path: {}", userId, request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");

            String errorJson = "{\"code\":\"BANNED\",\"message\":\"账号已被封禁，请联系管理员\"}";
            response.getWriter().write(errorJson);
            response.getWriter().flush();

            return false;
        }

        // 4. 用户未被封号，放行请求
        return true;
    }
}
