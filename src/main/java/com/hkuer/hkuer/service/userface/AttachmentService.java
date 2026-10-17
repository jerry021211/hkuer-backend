package com.hkuer.hkuer.service.userface;

import java.util.List;

/**
 * 附件（图片）生命周期管理服务
 */
public interface AttachmentService {

    /**
     * 记录上传的附件（状态为 PENDING=0）
     *
     * @param url 文件完整 URL
     */
    void savePending(String url);

    /**
     * 将指定 URL 列表的附件转为正式状态（ACTIVE=1）
     *
     * @param urls 需要转正的文件 URL 列表
     */
    void activateByUrls(List<String> urls);

    /**
     * 清理所有临时附件（status=0）
     * 同时删除 OSS 上的文件和数据库记录
     *
     * @return 清理的记录数
     */
    int cleanAllPending();
}
