package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员审核申诉请求 DTO
 */
@Data
public class PostAppealAuditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 申诉ID
     */
    private Long appealId;

    /**
     * 是否通过：true-通过, false-驳回
     */
    private Boolean pass;

    /**
     * 驳回原因（驳回时必填）
     */
    private String reply;
}
