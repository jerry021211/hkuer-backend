package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程/老师评论返回对象，包含用户昵称和头像
 */
@Data
public class TeacherCommentResponse {

    private Long id;

    private String userId;

    private String nickname;

    private String avatar;

    private String content;

    private LocalDateTime createdAt;
}

