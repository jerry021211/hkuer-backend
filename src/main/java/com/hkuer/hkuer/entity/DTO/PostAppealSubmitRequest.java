package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

import java.io.Serializable;

/**
 * 提交帖子申诉请求 DTO
 */
@Data
public class PostAppealSubmitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 申诉理由
     */
    private String reason;
}
