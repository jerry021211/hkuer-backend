package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户资料请求 DTO
 */
@Data
public class UpdateProfileRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;
}
