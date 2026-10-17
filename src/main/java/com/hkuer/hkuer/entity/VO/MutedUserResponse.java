package com.hkuer.hkuer.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 被禁言用户响应类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MutedUserResponse implements Serializable {
    private static final long serialVersionUID = 1L;

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
     * 禁言结束时间
     */
    private LocalDateTime muteEndTime;

    /**
     * 剩余禁言时间（分钟）
     */
    private Long remainingMinutes;

    /**
     * 禁言状态描述
     */
    private String statusDesc;
}
