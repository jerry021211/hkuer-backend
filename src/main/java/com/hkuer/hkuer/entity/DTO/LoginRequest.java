package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
