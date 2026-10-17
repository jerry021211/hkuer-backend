package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

@Data
public class UserInsertRequest {
    private String username;
    private String password; // 用户注册时的密码
    private String nickname;
    private String email; // 邮箱
    private String verificationCode; // 邮箱验证码
    /**
     * 头像链接（可选）
     */
    private String avatar;
}
