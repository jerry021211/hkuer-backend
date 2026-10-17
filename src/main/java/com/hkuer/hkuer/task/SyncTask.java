package com.hkuer.hkuer.task;

import com.hkuer.hkuer.service.userface.AttachmentService;
import com.hkuer.hkuer.service.userface.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling // 开启定时任务支持
public class SyncTask {

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private PostService postService;

    @org.springframework.beans.factory.annotation.Value("${mesh.attachment-cleanup-enabled:true}")
    private boolean cleanupEnabled = true;

    /**
     * 定时清理临时附件
     * 每天中午 12:00 执行，清理所有 status=0 的临时记录
     * 同时删除 OSS 上的文件和数据库记录
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void cleanExpiredAttachments() {
        if (!cleanupEnabled) return;
        log.info("开始执行临时附件清理...");
        long start = System.currentTimeMillis();

        try {
            int count = attachmentService.cleanAllPending();
            log.info("临时附件清理完成，共清理 {} 条记录，耗时: {} ms",
                    count, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("临时附件清理任务执行异常", e);
        }
    }

    /**
     * 定时清理过期的置顶帖子
     * 每小时执行一次，将超过3天的置顶帖子恢复为普通状态（is_top=0, top_time=null）
     */
//    @Scheduled(cron = "0 0 * * * ?")
    @Scheduled(cron = "0 * * * * ?")
    public void cleanExpiredTopPosts() {
        log.info("开始执行过期置顶帖子清理...");
        long start = System.currentTimeMillis();

        try {
            int count = postService.cleanExpiredTopPosts();
            log.info("过期置顶帖子清理完成，共清理 {} 条记录，耗时: {} ms",
                    count, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("过期置顶帖子清理任务执行异常", e);
        }
    }
}
