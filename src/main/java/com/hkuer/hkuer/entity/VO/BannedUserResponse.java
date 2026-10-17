package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 被封号用户信息响应
 */
@Data
public class BannedUserResponse {
    
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
     * 封号结束时间
     */
    private LocalDateTime banEndTime;
    
    /**
     * 剩余封号时间（分钟）
     */
    private long remainingMinutes;
    
    /**
     * 状态描述（如：剩余 3 天 2 小时）
     */
    private String statusDesc;
}
