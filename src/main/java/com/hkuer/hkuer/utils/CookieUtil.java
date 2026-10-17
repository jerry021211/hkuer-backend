package com.hkuer.hkuer.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie工具类
 * 用于管理JWT Token的Cookie存储
 */
@Component
public class CookieUtil {

    @Value("${jwt.cookie.access-name:access_token}")
    private String ACCESS_TOKEN_COOKIE = "access_token";
    @Value("${jwt.cookie.refresh-name:refresh_token}")
    private String REFRESH_TOKEN_COOKIE = "refresh_token";
    
    @Value("${jwt.cookie.domain:}")
    private String cookieDomain;
    
    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;
    
    @Value("${jwt.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * 设置Access Token Cookie
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token, int maxAge) {
        ResponseCookie cookie = createResponseCookie(ACCESS_TOKEN_COOKIE, token, maxAge);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 设置Refresh Token Cookie
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token, int maxAge) {
        ResponseCookie cookie = createResponseCookie(REFRESH_TOKEN_COOKIE, token, maxAge);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 从请求中获取Access Token
     */
    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    /**
     * 从请求中获取Refresh Token
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    /**
     * 清除所有Token Cookie
     */
    public void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessTokenCookie = createResponseCookie(ACCESS_TOKEN_COOKIE, "", 0);
        ResponseCookie refreshTokenCookie = createResponseCookie(REFRESH_TOKEN_COOKIE, "", 0);
        response.addHeader("Set-Cookie", accessTokenCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
    }

    /**
     * 创建ResponseCookie（支持SameSite属性）
     */
    private ResponseCookie createResponseCookie(String name, String value, int maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true) // 防止XSS攻击
                .path("/")
                .maxAge(maxAge)
                .secure(cookieSecure); // HTTPS环境下设置为true
        
        // 设置Domain（如果配置了）
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            builder.domain(cookieDomain);
        }
        
        // 设置SameSite属性
        if ("Strict".equalsIgnoreCase(cookieSameSite)) {
            builder.sameSite("Strict");
        } else if ("None".equalsIgnoreCase(cookieSameSite)) {
            builder.sameSite("None");
        } else {
            builder.sameSite("Lax"); // 默认Lax
        }
        
        return builder.build();
    }

    /**
     * 从请求中获取指定名称的Cookie值
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
