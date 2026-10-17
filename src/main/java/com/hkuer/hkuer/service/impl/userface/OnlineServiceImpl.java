package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.service.userface.OnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 在线用户统计服务实现
 * 使用 Redis ZSet 滑动窗口机制
 * 
 * 原理：
 * - ZSet Key: online:users
 * - Member: userId
 * - Score: 用户最后活跃时间戳（毫秒）
 * 
 * 统计在线人数时，只统计 Score 在 [当前时间 - 窗口大小, 当前时间] 范围内的用户
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OnlineServiceImpl implements OnlineService {

    private final StringRedisTemplate stringRedisTemplate;
    
    // Redis Key
    private static final String ONLINE_USERS_KEY = "online:users";
    
    // 滑动窗口大小（毫秒），默认15分钟，用户15分钟内有活动视为在线
    @Value("${online.window-size:900000}")
    private long windowSize;

    @Override
    public void heartbeat(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        // 更新用户在 ZSet 中的 Score 为当前时间戳
        stringRedisTemplate.opsForZSet().add(ONLINE_USERS_KEY, userId, now);
    }

    @Override
    public long getOnlineCount() {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSize;
        
        // 统计 Score 在 [windowStart, now] 范围内的用户数量
        Long count = stringRedisTemplate.opsForZSet().count(ONLINE_USERS_KEY, windowStart, now);
        return count != null ? count : 0;
    }

    @Override
    public void offline(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        // 从 ZSet 中移除用户
        stringRedisTemplate.opsForZSet().remove(ONLINE_USERS_KEY, userId);
        log.debug("用户 {} 已下线", userId);
    }

    @Override
    @Scheduled(fixedDelay = 60000) // 每分钟清理一次
    public void cleanExpiredUsers() {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSize;
        
        // 移除 Score 小于 windowStart 的所有用户（即超出滑动窗口的用户）
        Long removed = stringRedisTemplate.opsForZSet().removeRangeByScore(ONLINE_USERS_KEY, 0, windowStart);
        if (removed != null && removed > 0) {
            log.debug("清理过期在线用户: {} 个", removed);
        }
    }
}
