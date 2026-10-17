package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.entity.VO.MutedUserResponse;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.MuteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 禁言服务实现类
 * 
 * 核心逻辑：
 * 1. 数据库存储持久化禁言状态（status, mute_end_time）
 * 2. Redis 缓存禁言标记，用于高性能拦截（Key: user:mute:{userId}）
 * 3. 禁言时同时写入数据库和Redis，Redis TTL 与禁言时长一致
 * 4. 解禁时同时更新数据库和删除Redis Key
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MuteServiceImpl implements MuteService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Redis Key 前缀：用户禁言标记
     */
    private static final String MUTE_KEY_PREFIX = "user:mute:";

    /**
     * 禁言状态值
     */
    private static final Integer STATUS_NORMAL = 0;
    private static final Integer STATUS_MUTED = 1;
    private static final Integer STATUS_BANNED = 2;

    @Override
    public boolean isMuted(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        String key = MUTE_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public long getMuteRemainingMinutes(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return 0;
        }
        String key = MUTE_KEY_PREFIX + userId;
        Long ttlSeconds = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return 0;
        }
        // 转换为分钟，向上取整
        return (ttlSeconds + 59) / 60;
    }

    @Override
    @Transactional
    public Result muteUser(String userId, int days) {
        // 1. 参数校验
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("400", "用户ID不能为空");
        }
        if (days <= 0) {
            return Result.error("400", "禁言天数必须大于0");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("404", "用户不存在");
        }

        // 3. 检查用户是否被封号
        // 如果用户被封号（status=2），禁言时只更新 mute_end_time，保持 status=2
        // 这样解封后如果禁言还没过期，可以继续禁言
        boolean isBanned = (user.getStatus() != null && user.getStatus().equals(STATUS_BANNED));

        // 4. 计算禁言结束时间
        LocalDateTime muteEndTime = LocalDateTime.now().plusDays(days);

        // 5. 更新数据库
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<User>()
                .eq("id", userId)
                .set("mute_end_time", muteEndTime);

        // 如果用户未被封号，才将 status 设置为 1（禁言）
        // 如果用户被封号，保持 status=2（封号优先级高于禁言）
        if (!isBanned) {
            updateWrapper.set("status", STATUS_MUTED);
        }

        int updateCount = userMapper.update(null, updateWrapper);

        if (updateCount == 0) {
            return Result.error("500", "禁言失败，数据库更新失败");
        }

        // 6. 写入 Redis：Key=user:mute:{userId}, Value="1", TTL=days天
        String key = MUTE_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, "1", days, TimeUnit.DAYS);

        log.info("用户 {} 已被禁言 {} 天，解禁时间: {}{}", userId, days, muteEndTime,
                isBanned ? "（用户仍被封号）" : "");
        return Result.success("禁言成功，解禁时间: " + muteEndTime);
    }

    @Override
    @Transactional
    public Result unmuteUser(String userId) {
        // 1. 参数校验
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("400", "用户ID不能为空");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("404", "用户不存在");
        }

        // 3. 检查用户是否被封号
        // 如果用户被封号（status=2），解除禁言时只删除禁言相关字段，保持 status=2
        Integer currentStatus = user.getStatus();
        boolean isBanned = (currentStatus != null && currentStatus.equals(STATUS_BANNED));

        // 4. 更新数据库
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<User>()
                .eq("id", userId)
                .set("mute_end_time", null);

        // 如果用户未被封号，才将 status 设置为 0
        // 如果用户被封号，保持 status=2（封号优先级高于禁言）
        if (!isBanned) {
            updateWrapper.set("status", STATUS_NORMAL);
        }

        int updateCount = userMapper.update(null, updateWrapper);

        if (updateCount == 0) {
            return Result.error("500", "解禁失败，数据库更新失败");
        }

        // 5. 删除 Redis Key
        String key = MUTE_KEY_PREFIX + userId;
        stringRedisTemplate.delete(key);

        log.info("用户 {} 已解除禁言{}", userId, isBanned ? "（用户仍被封号）" : "");
        return Result.success("解禁成功");
    }

    @Override
    public void syncMuteStatusToRedis(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        // 1. 从数据库查询用户禁言状态
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        // 2. 如果用户状态是正常（status=0），无需处理
        if (user.getStatus() == null || user.getStatus().equals(STATUS_NORMAL)) {
            // 确保 Redis 中也没有禁言标记
            String key = MUTE_KEY_PREFIX + userId;
            stringRedisTemplate.delete(key);
            return;
        }

        // 3. 用户状态是禁言（status=1）或封号（status=2），检查禁言是否过期
        // 注意：如果用户被封号，status=2，但可能同时有 mute_end_time（封号前被禁言）
        LocalDateTime muteEndTime = user.getMuteEndTime();
        LocalDateTime now = LocalDateTime.now();

        if (muteEndTime == null || muteEndTime.isBefore(now)) {
            // 禁言已过期，自动解禁
            // 检查用户是否被封号，如果被封号，保持 status=2（封号优先级高于禁言）
            boolean isBanned = (user.getStatus() != null && user.getStatus().equals(STATUS_BANNED));

            UpdateWrapper<User> updateWrapper = new UpdateWrapper<User>()
                    .eq("id", userId)
                    .set("mute_end_time", null);

            // 如果用户未被封号，才将 status 设置为 0
            if (!isBanned) {
                updateWrapper.set("status", STATUS_NORMAL);
            }

            userMapper.update(null, updateWrapper);

            // 删除可能存在的 Redis Key
            String key = MUTE_KEY_PREFIX + userId;
            stringRedisTemplate.delete(key);

            log.info("用户 {} 禁言已过期，自动解禁{}", userId, isBanned ? "（用户仍被封号）" : "");
            return;
        }

        // 4. 禁言未过期，检查 Redis 中是否存在 Key
        String key = MUTE_KEY_PREFIX + userId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            // Redis 中已存在，无需处理
            return;
        }

        // 5. Redis 中不存在（可能 Redis 重启过），重新设置
        // 计算剩余禁言时间（秒）
        long remainingSeconds = ChronoUnit.SECONDS.between(now, muteEndTime);
        if (remainingSeconds > 0) {
            stringRedisTemplate.opsForValue().set(key, "1", remainingSeconds, TimeUnit.SECONDS);
            log.info("用户 {} 禁言状态已同步到 Redis，剩余 {} 秒", userId, remainingSeconds);
        }
    }

    @Override
    public List<MutedUserResponse> getMutedUsers() {
        // 1. 查询数据库中所有被禁言的用户（status=1）
        List<User> mutedUsers = userMapper.selectList(new QueryWrapper<User>()
                .eq("status", STATUS_MUTED)
                .isNotNull("mute_end_time")
                .orderByDesc("mute_end_time"));

        List<MutedUserResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 2. 遍历用户，构建响应对象
        for (User user : mutedUsers) {
            LocalDateTime muteEndTime = user.getMuteEndTime();

            // 如果禁言已过期，跳过（理论上应该被自动解禁了，但为了安全还是检查）
            if (muteEndTime == null || muteEndTime.isBefore(now)) {
                continue;
            }

            // 计算剩余禁言时间（分钟）
            long remainingMinutes = ChronoUnit.MINUTES.between(now, muteEndTime);

            // 构建状态描述
            String statusDesc;
            if (remainingMinutes >= 1440) {
                // 超过24小时，显示天数
                long days = remainingMinutes / 1440;
                long hours = (remainingMinutes % 1440) / 60;
                statusDesc = String.format("剩余 %d 天 %d 小时", days, hours);
            } else if (remainingMinutes >= 60) {
                // 超过1小时，显示小时和分钟
                long hours = remainingMinutes / 60;
                long minutes = remainingMinutes % 60;
                statusDesc = String.format("剩余 %d 小时 %d 分钟", hours, minutes);
            } else {
                // 少于1小时，只显示分钟
                statusDesc = String.format("剩余 %d 分钟", remainingMinutes);
            }

            // 构建响应对象
            MutedUserResponse response = new MutedUserResponse();
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setEmail(user.getEmail());
            response.setMuteEndTime(muteEndTime);
            response.setRemainingMinutes(remainingMinutes);
            response.setStatusDesc(statusDesc);

            result.add(response);
        }

        return result;
    }
}
