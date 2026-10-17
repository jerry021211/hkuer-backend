package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.LoginRequest;
import com.hkuer.hkuer.entity.DTO.LoginResponse;
import com.hkuer.hkuer.entity.DTO.SendVerificationCodeRequest;
import com.hkuer.hkuer.entity.DTO.UpdateProfileRequest;
import com.hkuer.hkuer.entity.DTO.UserInsertRequest;
import com.hkuer.hkuer.entity.VO.UserMetaResponse;
import com.hkuer.hkuer.entity.VO.ProfileResponse;
import com.hkuer.hkuer.entity.VO.UserTypeResponse;
import com.hkuer.hkuer.service.userface.OnlineService;
import com.hkuer.hkuer.service.userface.UserService;
import com.hkuer.hkuer.utils.CookieUtil;
import com.hkuer.hkuer.utils.JwtUtil;
import com.hkuer.hkuer.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;
    private final OnlineService onlineService;

    @Value("${jwt.expiration:300000}")
    private Long accessTokenExpiration; // Access Token过期时间（毫秒），默认5分钟

    @Value("${jwt.refresh-expiration:600000}")
    private Long refreshTokenExpiration; // Refresh Token过期时间（毫秒），默认10分钟

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/sendverificationcode")
    public Result sendVerificationCode(@RequestBody SendVerificationCodeRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return Result.error("400", "邮箱不能为空");
        }
        return userService.sendVerificationCode(request.getEmail());
    }

    @PostMapping("/insert")
    public Result insert(@RequestBody UserInsertRequest request) {
        return userService.insertUser(request);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request, HttpServletResponse response) {
        Result result = userService.login(request);

        // 如果登录成功，将token存入Cookie
        if (result.getCode().equals("200") && result.getData() != null) {
            LoginResponse loginResponse = (LoginResponse) result.getData();

            // 计算Cookie过期时间（秒）
            int accessTokenMaxAge = (int) (accessTokenExpiration / 1000);
            int refreshTokenMaxAge = (int) (refreshTokenExpiration / 1000);

            // 设置Cookie
            cookieUtil.setAccessTokenCookie(response, loginResponse.getAccessToken(), accessTokenMaxAge);
            cookieUtil.setRefreshTokenCookie(response, loginResponse.getRefreshToken(), refreshTokenMaxAge);

            // 返回响应时，可以选择不清除token字段（前端仍可使用）
            // 或者可以选择不返回token，只返回用户信息（更安全）
            // 这里保留原逻辑，但主要使用Cookie
        }

        return result;
    }

    @GetMapping("/public-key")
    public Result getPublicKey() {
        return userService.getPublicKey();
    }

    @PostMapping("/refresh")
    public Result refresh(@RequestBody(required = false) java.util.Map<String, String> requestBody,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        // 优先从Cookie获取refreshToken
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(httpRequest);

        // 如果Cookie中没有，尝试从请求体获取（兼容旧接口）
        if ((refreshToken == null || refreshToken.isEmpty()) && requestBody != null) {
            refreshToken = requestBody.get("refreshToken");
        }

        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.error("401", "请重新登录");
        }

        Result result = userService.refreshToken(refreshToken);

        // 如果刷新成功，更新Cookie中的accessToken
        if (result.getCode().equals("200") && result.getData() != null) {
            LoginResponse loginResponse = (LoginResponse) result.getData();

            // 计算Cookie过期时间（秒）
            int accessTokenMaxAge = (int) (accessTokenExpiration / 1000);

            // 更新Access Token Cookie
            cookieUtil.setAccessTokenCookie(response, loginResponse.getAccessToken(), accessTokenMaxAge);
        }

        return result;
    }

    @GetMapping("/whoami_frontend")
    public Result whoamiFrontend() {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("500", "用户id未找到");
        }
        try {
            UserMetaResponse userDetails = userService.getUserMetaData(userId);
            if (userDetails == null) {
                return Result.error("500", "用户未找到");
            }
            return Result.success(userDetails);
        } catch (Exception e) {
            return Result.error("500", "获取用户基本信息失败");
        }
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        // 获取当前用户ID（用于移除在线状态）
        String userId = UserContext.getCurrentUserId();

        // 从 Cookie 中获取 RefreshToken
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);

        // 从 Redis 中删除 RefreshToken
        userService.logout(refreshToken);

        // 将用户从在线集合中移除
        if (userId != null && !userId.isEmpty()) {
            onlineService.offline(userId);
        }

        // 清除 Cookie
        cookieUtil.clearTokenCookies(response);
        return Result.success("退出登录成功");
    }

    /**
     * 测试接口：检查当前Token信息
     * 用于调试Token的过期时间等信息
     */
    @GetMapping("/token-info")
    public Result getTokenInfo(HttpServletRequest request) {
        // 从请求中获取Token
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isEmpty()) {
            // 如果Cookie中没有，尝试从请求头获取
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                token = bearerToken.substring(7);
            }
        }

        if (token == null || token.isEmpty()) {
            return Result.error("400", "未找到Token");
        }

        // 验证Token
        boolean isValid = jwtUtil.validateToken(token);
        String userId = jwtUtil.getUserIdFromToken(token);
        java.util.Date expiration = jwtUtil.getExpirationFromToken(token);
        java.util.Date issuedAt = jwtUtil.getIssuedAtFromToken(token);

        // 计算剩余时间
        long remainingTime = 0;
        boolean isExpired = false;
        if (expiration != null) {
            long now = System.currentTimeMillis();
            long expTime = expiration.getTime();
            remainingTime = expTime - now;
            isExpired = remainingTime <= 0;
        }

        // 构建响应
        java.util.Map<String, Object> tokenInfo = new java.util.HashMap<>();
        tokenInfo.put("isValid", isValid);
        tokenInfo.put("isExpired", isExpired);
        tokenInfo.put("userId", userId);
        tokenInfo.put("issuedAt", issuedAt != null ? issuedAt.toString() : null);
        tokenInfo.put("expiration", expiration != null ? expiration.toString() : null);
        tokenInfo.put("remainingTimeMs", remainingTime);
        tokenInfo.put("remainingTimeMinutes", remainingTime > 0 ? remainingTime / 60000.0 : 0);

        return Result.success(tokenInfo);
    }

    /**
     * 前端获取当前用户的 usertype
     */
    @GetMapping("/get_usertype")
    public Result getCurrentUserType() {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("401", "未登录");
        }

        try {
            UserTypeResponse userType = userService.getUserType(userId);
            if (userType == null) {
                return Result.error("404", "用户不存在");
            }
            return Result.success(userType);
        } catch (Exception e) {
            return Result.error("500", "获取用户类型失败");
        }
    }

    /**
     * 获取当前登录用户的资料（昵称和头像）
     */
    @GetMapping("/profile")
    public Result getProfile() {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("401", "未登录");
        }

        try {
            ProfileResponse profile = userService.getProfile(userId);
            if (profile == null) {
                return Result.error("404", "用户不存在");
            }
            return Result.success(profile);
        } catch (Exception e) {
            return Result.error("500", "获取用户资料失败");
        }
    }

    /**
     * 更新当前登录用户的资料（昵称和头像）
     */
    @PostMapping("/profile/update")
    public Result updateProfile(@RequestBody UpdateProfileRequest request) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("401", "未登录");
        }

        // 参数校验：至少提供一个字段
        if ((request.getNickname() == null || request.getNickname().trim().isEmpty())
                && (request.getAvatar() == null || request.getAvatar().trim().isEmpty())) {
            return Result.error("400", "至少需要提供一个更新字段（昵称或头像）");
        }

        return userService.updateProfile(userId, request);
    }

}