package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "bearer";
    private String userId;
    private String username;
    private String nickname;
}
