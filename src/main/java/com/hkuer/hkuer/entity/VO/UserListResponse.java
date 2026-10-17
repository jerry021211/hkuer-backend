package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表响应VO
 * 用于返回用户信息，不包含敏感信息（密码、验证码）
 */
@Data
public class UserListResponse {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户类型：0-普通用户, 1-管理员
     */
    private Integer usertype;

    /**
     * 用户状态：0-正常, 1-禁言, 2-封号
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 禁言结束时间
     */
    private LocalDateTime muteEndTime;

    /**
     * 封号结束时间
     */
    private LocalDateTime banEndTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
