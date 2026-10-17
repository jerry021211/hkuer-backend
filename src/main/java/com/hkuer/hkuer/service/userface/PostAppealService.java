package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.PostAppealAuditRequest;
import com.hkuer.hkuer.entity.DTO.PostAppealSubmitRequest;

/**
 * 帖子申诉服务接口
 */
public interface PostAppealService {

    /**
     * 提交申诉
     *
     * @param request 申诉请求
     * @return 结果
     */
    Result submitAppeal(PostAppealSubmitRequest request);

    /**
     * 管理员审核申诉
     *
     * @param request 审核请求
     * @return 结果
     */
    Result auditAppeal(PostAppealAuditRequest request);

    /**
     * 获取所有待处理的申诉列表（status = 0）
     *
     * @return 待处理申诉列表
     */
    Result getPendingAppeals();

    /**
     * 获取申诉总数
     *
     * @return 申诉总量
     */
    Long getAppealCount();

    /**
     * 获取待处理申诉数量（status = 0）
     *
     * @return 待处理申诉数量
     */
    Long getPendingAppealCount();
}
