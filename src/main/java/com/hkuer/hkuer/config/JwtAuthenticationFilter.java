package com.hkuer.hkuer.config;

import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.OnlineService;
import com.hkuer.hkuer.utils.CookieUtil;
import com.hkuer.hkuer.utils.JwtUtil;
import com.hkuer.hkuer.utils.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 优先从Cookie中获取Token，如果没有则从请求头获取
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final OnlineService onlineService;
    private final UserMapper userMapper;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TOKEN_PREFIX_LOWER = "bearer ";

    // 白名单路径（不需要Token验证）
    private static final String[] WHITELIST_PATHS = {
            "/mesh/public/password/code",
            "/mesh/public/password/reset",
            "/user/public-key",
            "/user/insert",
            "/user/login",
            "/user/refresh",
            "/user/logout", // 退出登录不需要token（允许未登录用户清除cookie）
            "/user/send-verification-code", // 发送验证码（注册时不需要登录）
            "/user/sendverificationcode", // 发送验证码（兼容不同路径格式）
            "/online/count", // 获取在线人数（无需登录）
            "/api/pay/notify", // 支付回调接口（第三方平台回调，无法携带Token）
            "/file/upload" // 上传图片接口（注册时上传头像等，允许未登录访问）
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            // 跳过 CORS 预检请求 (OPTIONS)，避免预检被当成普通请求拦截返回 401
            // 实际的跨域响应头由 CorsFilter 处理
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                chain.doFilter(request, response);
                return;
            }

            String requestPath = request.getRequestURI();
            boolean isWhitelist = isWhitelistPath(requestPath)
                    || ("GET".equals(request.getMethod()) && (requestPath.startsWith("/mesh/public/") || requestPath.startsWith("/media/")));

            // 非白名单路径必须验证Token
            if (!isWhitelist) {
                String token = getTokenFromRequest(request);

                // 检查Token是否存在且有效
                if (!StringUtils.hasText(token) || (!jwtUtil.validateToken(token) || "refresh".equals(jwtUtil.tokenType(token)))) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"401\",\"message\":\"未登录或Token无效\"}");
                    return;
                }

                // 从Token中获取用户ID；业务身份只信任 JWT subject。
                String userId = jwtUtil.getUserIdFromToken(token);
                if (!StringUtils.hasText(userId)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"401\",\"message\":\"Token解析失败\"}");
                    return;
                }

                UserContext.setCurrentUserId(userId);
                onlineService.heartbeat(userId);

                // 检查是否为管理员接口（/console 开头）
                if (isAdminPath(requestPath) && !isAdmin(userId)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"403\",\"message\":\"权限不足，仅管理员可访问\"}");
                    return;
                }
            } else {
                // 白名单路径：如果有Token则解析，但不强制要求
                String token = getTokenFromRequest(request);
                if (StringUtils.hasText(token) && jwtUtil.validateToken(token) && !"refresh".equals(jwtUtil.tokenType(token))) {
                    String userId = jwtUtil.getUserIdFromToken(token);
                    if (StringUtils.hasText(userId)) {
                        UserContext.setCurrentUserId(userId);
                        // 即使是白名单路径，有有效Token也更新在线状态
                        onlineService.heartbeat(userId);
                    }
                }
            }

            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 检查是否为白名单路径
     */
    private boolean isWhitelistPath(String path) {
        for (String whitelist : WHITELIST_PATHS) {
            if (path.equals(whitelist)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为管理员接口路径（以 /console 开头）
     */
    private boolean isAdminPath(String path) {
        return path != null && path.startsWith("/console");
    }

    /**
     * 检查用户是否为管理员（usertype = 1）
     */
    private boolean isAdmin(String userId) {
        if (userId == null) {
            return false;
        }
        try {
            User user = userMapper.selectById(userId);
            return user != null && user.getUsertype() != null && user.getUsertype() == 1;
        } catch (Exception e) {
            log.error("查询用户权限失败，userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 从请求中获取Token
     * 优先级：Cookie > 请求头
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 优先从Cookie获取
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (StringUtils.hasText(token)) {
            return token;
        }

        // 如果Cookie中没有，从请求头获取（兼容移动端 / 跨域场景）
        // Bearer 前缀大小写不敏感（前端 tokenType 可能是 "Bearer" 或 "bearer"）
        String bearerToken = request.getHeader(TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken)) {
            if (bearerToken.startsWith(TOKEN_PREFIX)) {
                return bearerToken.substring(TOKEN_PREFIX.length());
            }
            if (bearerToken.startsWith(TOKEN_PREFIX_LOWER)) {
                return bearerToken.substring(TOKEN_PREFIX_LOWER.length());
            }
        }

        return null;
    }
}
