package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hkuer.hkuer.entity.PO.Attachment;
import com.hkuer.hkuer.mapper.userface.AttachmentMapper;
import com.hkuer.hkuer.service.userface.AttachmentService;
import com.hkuer.hkuer.service.userface.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 附件生命周期管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final OssService ossService;

    @Override
    public void savePending(String url) {
        Attachment attachment = new Attachment();
        attachment.setUrl(url);
        attachment.setStatus(0); // PENDING
        attachment.setCreateTime(LocalDateTime.now().withNano(0));
        attachmentMapper.insert(attachment);
    }

    @Override
    @Transactional
    public void activateByUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        // 批量将匹配的 URL 记录更新为 ACTIVE(1)
        attachmentMapper.update(null,
                new LambdaUpdateWrapper<Attachment>()
                        .set(Attachment::getStatus, 1)
                        .eq(Attachment::getStatus, 0)
                        .in(Attachment::getUrl, urls));
    }

    @Override
    @Transactional
    public int cleanAllPending() {
        // 1. 查出所有 status=0 的临时附件
        List<Attachment> pendingList = attachmentMapper.selectList(
                new LambdaQueryWrapper<Attachment>()
                        .eq(Attachment::getStatus, 0));

        if (pendingList.isEmpty()) {
            return 0;
        }

        // 2. 逐条删除 OSS 文件
        for (Attachment att : pendingList) {
            try {
                ossService.deleteFile(att.getUrl());
                log.info("清理临时 OSS 文件: {}", att.getUrl());
            } catch (Exception e) {
                log.warn("删除 OSS 文件失败, url={}, error={}", att.getUrl(), e.getMessage());
            }
        }

        // 3. 批量删除数据库记录
        List<Long> ids = pendingList.stream()
                .map(Attachment::getId)
                .toList();
        attachmentMapper.deleteBatchIds(ids);

        return pendingList.size();
    }
}
