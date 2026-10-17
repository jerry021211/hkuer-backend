package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

@Data
public class SendVerificationCodeRequest {
    private String email;  // 接收验证码的邮箱
}

