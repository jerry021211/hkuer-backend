package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

/**
 * 处理举报请求DTO
 */
@Data
public class ReportHandleRequest {

    /**
     * 举报ID（字符串格式）
     */
    private String reportId;

    /**
     * 处理状态: 1-已处罚/有效, 2-已驳回/无效
     */
    private Integer status;

    /**
     * 处理结果备注（可选）
     */
    private String handleNote;
}
