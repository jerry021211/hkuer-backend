package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.ReportHandleRequest;
import com.hkuer.hkuer.entity.DTO.ReportInsertRequest;

import java.util.Map;

/**
 * 举报服务接口
 */
public interface ReportService {
    
    /**
     * 提交举报
     *
     * @param request 举报请求
     * @return 结果
     */
    Result submitReport(ReportInsertRequest request);
    
    /**
     * 处理举报
     *
     * @param request 处理请求
     * @return 结果
     */
    Result handleReport(ReportHandleRequest request);
    
    /**
     * 获取举报数量
     *
     * @return 举报数量
     */
    Long getReportCount();
    
    /**
     * 获取待处理举报数量
     *
     * @return 待处理举报数量
     */
    Long getPendingReportCount();

    /**
     * 分页获取待处理举报列表（status=0）
     *
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 分页结果（包含 records, total, page, size, pages）
     */
    Map<String, Object> getPendingReports(Integer page, Integer size);
}
