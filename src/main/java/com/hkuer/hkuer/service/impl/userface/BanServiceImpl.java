package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.entity.VO.BannedUserResponse;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.BanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户封号服务实现类
 * 
 * 核心逻辑：
 * 1. 数据库存储持久化封号状态（status=2, ban_end_time）
 * 2. Redis 黑名单缓存，用于高性能拦截（Key: user:banned:{userId}）
 * 3. 封号时同时写入数据库和Redis，Redis TTL 与封号时长一致
 * 4. 解封时同时更新数据库和删除Redis Key
 * 5. 全局拦截器检查Redis黑名单，实现立即踢人下线
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BanServiceImpl implements BanService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Redis Key 前缀：用户封号黑名单
     */
    private static final String BAN_KEY_PREFIX = "user:banned:";

    /**
     * 用户状态常量
     */
    private static final Integer STATUS_NORMAL = 0;
    private static final Integer STATUS_MUTED = 1;
    private static final Integer STATUS_BANNED = 2;

    @Override
    public boolean isBanned(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        String key = BAN_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public long getBanRemainingMinutes(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return 0;
        }
        String key = BAN_KEY_PREFIX + userId;
        Long ttlSeconds = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return 0;
        }
        // 转换为分钟，向上取整
        return (ttlSeconds + 59) / 60;
    }

    @Override
    @Transactional
    public Result banUser(String userId, int days) {
        // 1. 参数校验
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("400", "用户ID不能为空");
        }
        if (days <= 0) {
            return Result.error("400", "封号天数必须大于0");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("404", "用户不存在");
        }

        // 3. 检查是否是管理员（不允许封禁管理员）
        if (user.getUsertype() != null && user.getUsertype() == 1) {
            return Result.error("403", "不能封禁管理员账号");
        }

        // 4. 计算封号结束时间
        LocalDateTime banEndTime = LocalDateTime.now().plusDays(days);

        // 5. 更新数据库：设置 status=2 和 ban_end_time
        int updateCount = userMapper.update(null, new UpdateWrapper<User>()
                .eq("id", userId)
                .set("status", STATUS_BANNED)
                .set("ban_end_time", banEndTime));

        if (updateCount == 0) {
            return Result.error("500", "封号失败，数据库更新失败");
        }

        // 6. 写入 Redis 黑名单：Key=user:banned:{userId}, Value="1", TTL=days天
        // 这是实现"立即踢人下线"的关键！
        String key = BAN_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, "1", days, TimeUnit.DAYS);

        // 7. 同时删除该用户的所有 RefreshToken（可选：让用户无法刷新 Token）
        // 这里可以通过 pattern 删除，但为了简单起见，依赖拦截器检查

        log.info("用户 {} 已被封号 {} 天，解封时间: {}", userId, days, banEndTime);
        return Result.success("封号成功，解封时间: " + banEndTime);
    }

    @Override
    @Transactional
    public Result unbanUser(String userId) {
        // 1. 参数校验
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("400", "用户ID不能为空");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("404", "用户不存在");
        }

        // 3. 检查用户是否被封号（同时检查数据库和 Redis）
        // 数据库状态为封号，或者 Redis 中有封号标记，都认为用户被封号
        String key = BAN_KEY_PREFIX + userId;
        boolean isBannedInRedis = Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        boolean isBannedInDb = (user.getStatus() != null && user.getStatus().equals(STATUS_BANNED));
        
        if (!isBannedInDb && !isBannedInRedis) {
            return Result.error("400", "该用户未被封号");
        }

        // 4. 检查用户是否还有禁言状态（解封后应该恢复禁言状态，而不是直接变成正常）
        LocalDateTime muteEndTime = user.getMuteEndTime();
        LocalDateTime now = LocalDateTime.now();
        boolean hasActiveMute = (muteEndTime != null && muteEndTime.isAfter(now));
        
        // 5. 更新数据库：清空 ban_end_time，根据禁言状态设置 status
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<User>()
                .eq("id", userId)
                .set("ban_end_time", null);
        
        // 如果用户还有有效的禁言，解封后应该恢复禁言状态（status=1）
        // 如果用户没有禁言或禁言已过期，解封后恢复正常状态（status=0）
        if (hasActiveMute) {
            updateWrapper.set("status", STATUS_MUTED);
        } else {
            updateWrapper.set("status", STATUS_NORMAL);
            // 如果禁言已过期，也清空 mute_end_time
            updateWrapper.set("mute_end_time", null);
        }
        
        userMapper.update(null, updateWrapper);

        // 6. 删除 Redis 黑名单（无论是否存在都删除，确保数据一致性）
        stringRedisTemplate.delete(key);

        // 7. 如果用户还有有效的禁言，需要同步禁言状态到 Redis
        if (hasActiveMute) {
            // 计算剩余禁言时间（秒）
            long remainingSeconds = ChronoUnit.SECONDS.between(now, muteEndTime);
            if (remainingSeconds > 0) {
                // 设置 Redis 禁言标记（使用 MuteService 的 Key 格式）
                String muteKey = "user:mute:" + userId;
                stringRedisTemplate.opsForValue().set(muteKey, "1", remainingSeconds, TimeUnit.SECONDS);
                log.info("用户 {} 解封后恢复禁言状态，剩余 {} 秒", userId, remainingSeconds);
            }
        }

        log.info("用户 {} 已解除封号（数据库状态: {}, Redis状态: {}, 禁言状态: {}）", userId, 
                isBannedInDb ? "封号" : "正常", 
                isBannedInRedis ? "封号" : "正常",
                hasActiveMute ? "禁言中" : "正常");
        return Result.success("解封成功" + (hasActiveMute ? "，用户仍处于禁言状态" : ""));
    }

    @Override
    public boolean syncBanStatusToRedis(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        // 1. 从数据库查询用户状态
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        // 2. 如果用户状态不是封号（status != 2），返回 false
        if (user.getStatus() == null || !user.getStatus().equals(STATUS_BANNED)) {
            // 确保 Redis 中也没有封号标记
            String key = BAN_KEY_PREFIX + userId;
            stringRedisTemplate.delete(key);
            return false;
        }

        // 3. 用户状态是封号（status=2），检查封号是否过期
        LocalDateTime banEndTime = user.getBanEndTime();
        LocalDateTime now = LocalDateTime.now();

        if (banEndTime == null || banEndTime.isBefore(now)) {
            // 封号已过期，自动解封
            userMapper.update(null, new UpdateWrapper<User>()
                    .eq("id", userId)
                    .set("status", STATUS_NORMAL)
                    .set("ban_end_time", null));

            // 删除可能存在的 Redis Key
            String key = BAN_KEY_PREFIX + userId;
            stringRedisTemplate.delete(key);

            log.info("用户 {} 封号已过期，自动解封", userId);
            return false; // 已自动解封，用户可以登录
        }

        // 4. 封号未过期，检查 Redis 中是否存在 Key
        String key = BAN_KEY_PREFIX + userId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            // Redis 中已存在，用户仍被封号
            return true;
        }

        // 5. Redis 中不存在（可能 Redis 重启过），重新设置
        long remainingSeconds = ChronoUnit.SECONDS.between(now, banEndTime);
        if (remainingSeconds > 0) {
            stringRedisTemplate.opsForValue().set(key, "1", remainingSeconds, TimeUnit.SECONDS);
            log.info("用户 {} 封号状态已同步到 Redis，剩余 {} 秒", userId, remainingSeconds);
        }

        return true; // 用户仍被封号
    }

    @Override
    public List<BannedUserResponse> getBannedUsers() {
        // 查询所有被封号的用户（status=2）
        List<User> bannedUsers = userMapper.selectList(new QueryWrapper<User>()
                .eq("status", STATUS_BANNED)
                .isNotNull("ban_end_time"));

        List<BannedUserResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (User user : bannedUsers) {
            LocalDateTime banEndTime = user.getBanEndTime();

            // 如果封号已过期，跳过
            if (banEndTime == null || banEndTime.isBefore(now)) {
                continue;
            }

            // 计算剩余封号时间（分钟）
            long remainingMinutes = ChronoUnit.MINUTES.between(now, banEndTime);

            // 构建状态描述
            String statusDesc;
            if (remainingMinutes >= 1440) {
                long days = remainingMinutes / 1440;
                long hours = (remainingMinutes % 1440) / 60;
                statusDesc = String.format("剩余 %d 天 %d 小时", days, hours);
            } else if (remainingMinutes >= 60) {
                long hours = remainingMinutes / 60;
                long minutes = remainingMinutes % 60;
                statusDesc = String.format("剩余 %d 小时 %d 分钟", hours, minutes);
            } else {
                statusDesc = String.format("剩余 %d 分钟", remainingMinutes);
            }

            // 构建响应对象
            BannedUserResponse response = new BannedUserResponse();
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setEmail(user.getEmail());
            response.setBanEndTime(banEndTime);
            response.setRemainingMinutes(remainingMinutes);
            response.setStatusDesc(statusDesc);

            result.add(response);
        }

        return result;
    }

    /**
     * 定时清理过期封号（每10分钟执行一次）
     * 
     * 作用：确保数据库中过期的封号状态被自动清理
     * 场景：用户被封号后一直没有尝试登录，导致数据库中的 status=2 一直保持
     * 通过定时任务主动检查并清理，保证数据一致性
     */
    @Scheduled(fixedDelay = 600000) // 每10分钟执行一次
    public void cleanupExpiredBans() {
        try {
            // 1. 查询所有被封号的用户（status=2 且 ban_end_time 不为空）
            List<User> bannedUsers = userMapper.selectList(new QueryWrapper<User>()
                    .eq("status", STATUS_BANNED)
                    .isNotNull("ban_end_time"));

            if (bannedUsers.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int cleanedCount = 0;

            // 2. 遍历检查每个用户的封号是否过期
            for (User user : bannedUsers) {
                LocalDateTime banEndTime = user.getBanEndTime();
                
                // 如果封号已过期，自动解封
                if (banEndTime != null && banEndTime.isBefore(now)) {
                    // 更新数据库：设置 status=0，清空 ban_end_time
                    userMapper.update(null, new UpdateWrapper<User>()
                            .eq("id", user.getId())
                            .set("status", STATUS_NORMAL)
                            .set("ban_end_time", null));

                    // 删除可能存在的 Redis Key（理论上应该已经过期，但为了保险起见）
                    String key = BAN_KEY_PREFIX + user.getId();
                    stringRedisTemplate.delete(key);

                    cleanedCount++;
                    log.info("定时任务：用户 {} 封号已过期，自动解封", user.getId());
                }
            }

            if (cleanedCount > 0) {
                log.info("定时任务：清理过期封号完成，共清理 {} 个用户", cleanedCount);
            }
        } catch (Exception e) {
            log.error("定时任务：清理过期封号失败", e);
        }
    }
}
