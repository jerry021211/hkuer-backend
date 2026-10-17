package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

/**
 * 提交举报请求DTO
 */
@Data
public class ReportInsertRequest {
    
    /**
     * 被举报目标ID（帖子ID、评论ID、用户ID）
     */
    private String targetId;
    
    /**
     * 目标类型: POST-帖子, COMMENT-评论, USER-用户
     */
    private String targetType;
    
    /**
     * 举报原因/类型
     */
    private String reason;
    
    /**
     * 详细描述（可选）
     */
    private String reasonDetail;
}
