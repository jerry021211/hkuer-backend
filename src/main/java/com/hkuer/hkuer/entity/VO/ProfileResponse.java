package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户资料响应 VO（昵称和头像）
 */
@Data
public class ProfileResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String nickname;
    private String avatar; // 头像 URL
}
