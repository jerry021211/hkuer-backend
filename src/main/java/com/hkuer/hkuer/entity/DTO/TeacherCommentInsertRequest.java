package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

/**
 * 用户评论老师 - 插入请求（不包含 userId，由后端从登录上下文获取）
 */
@Data
public class TeacherCommentInsertRequest {

    /** 被评论的教师 ID */
    private String teacherId;

    /** 评论内容 */
    private String content;
}
