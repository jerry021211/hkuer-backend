package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "user")
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;
    private String username;
    private String password; // 加密后的密码
    private String nickname;
    private String avatar; // 头像 URL
    private String email; // 邮箱
    private String verificationCode; // 邮箱验证码
    private Integer usertype; // 用户类型，默认值为0
    private Integer status; // 用户状态: 0-正常, 1-禁言, 2-封号
    private LocalDateTime muteEndTime; // 禁言结束时间
    private LocalDateTime banEndTime; // 封号结束时间
    private LocalDateTime createdAt;
}
