package com.hkuer.hkuer.service.userface;

/**
 * 浏览量统计服务接口
 */
public interface ViewCountService {
    
    /**
     * 异步增加浏览量（写入 Redis 缓冲）
     * @param postId 帖子ID
     * @param userId 用户ID（可为空，用于去重）
     */
    void incrementViewCountAsync(Long postId, String userId);
    
    /**
     * 获取帖子浏览量（优先从 Redis 获取）
     * @param postId 帖子ID
     * @return 浏览量
     */
    Long getViewCount(Long postId);
    
    /**
     * 定时将 Redis 中的浏览量同步到数据库
     */
    void syncViewCountToDb();
}
