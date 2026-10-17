package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报信息响应VO
 */
@Data
public class ReportResponse {
    
    /**
     * 举报ID
     */
    private Long id;
    
    /**
     * 举报人ID
     */
    private String reporterId;
    
    /**
     * 举报人用户名
     */
    private String reporterUsername;
    
    /**
     * 举报人昵称
     */
    private String reporterNickname;
    
    /**
     * 举报提交时间
     */
    private LocalDateTime createTime;
    
    /**
     * 被举报目标ID（帖子ID、评论ID、用户ID）
     */
    private String targetId;
    
    /**
     * 目标类型: POST-帖子, COMMENT-评论, USER-用户
     */
    private String targetType;
    
    /**
     * 被举报内容的作者ID
     */
    private String targetUserId;
    
    /**
     * 被举报内容的作者用户名
     */
    private String targetUsername;
    
    /**
     * 被举报内容的作者昵称
     */
    private String targetNickname;
    
    /**
     * 举报类型: 垃圾广告/违法违规/涉黄/人身攻击
     */
    private String reason;
    
    /**
     * 详细描述 (用户填写的补充说明)
     */
    private String reasonDetail;
    
    /**
     * 证据图片 (存图片URL数组)
     */
    private List<String> images;
    
    /**
     * 举报状态: 0-待处理, 1-已处罚/有效, 2-已驳回/无效
     */
    private Integer status;
    
    /**
     * 处理管理员ID
     */
    private String handlerId;
    
    /**
     * 处理时间
     */
    private LocalDateTime handleTime;
    
    /**
     * 处理结果备注
     */
    private String handleNote;
}
