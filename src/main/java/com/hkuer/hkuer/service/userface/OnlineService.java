package com.hkuer.hkuer.service.userface;

/**
 * 在线用户统计服务
 * 使用 Redis ZSet 滑动窗口机制
 */
public interface OnlineService {
    
    /**
     * 用户心跳 - 更新用户最后活跃时间
     * @param userId 用户ID
     */
    void heartbeat(String userId);
    
    /**
     * 获取当前在线人数
     * @return 在线人数
     */
    long getOnlineCount();
    
    /**
     * 用户下线 - 从在线集合中移除
     * @param userId 用户ID
     */
    void offline(String userId);
    
    /**
     * 清理过期用户（定时任务调用）
     */
    void cleanExpiredUsers();
}
