package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.service.userface.ViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ViewCountServiceImpl implements ViewCountService {

    private static final String ACTIVE_KEY = "hkuer:view:active";
    private static final String UNFLUSHED_KEY = "hkuer:view:unflushed";
    private static final String PENDING_KEY = "hkuer:view:pending";
    private static final String BATCH_PREFIX = "hkuer:view:batch:";
    private static final String DEDUPE_PREFIX = "hkuer:view:dedupe:";
    private static final String ANONYMOUS_PLACEHOLDER = "hkuer:view:anonymous";
    private static final int MAX_BATCHES_PER_RUN = 10;

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            if ARGV[2] == '1' then
                local firstView = redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[3])
                if not firstView then
                    return 0
                end
            end
            redis.call('HINCRBY', KEYS[2], ARGV[1], 1)
            redis.call('HINCRBY', KEYS[3], ARGV[1], 1)
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 0
            end
            redis.call('RENAME', KEYS[1], KEYS[2])
            redis.call('RPUSH', KEYS[3], ARGV[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> ACK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                redis.call('LREM', KEYS[3], 1, ARGV[1])
                return 0
            end
            local values = redis.call('HGETALL', KEYS[1])
            for i = 1, #values, 2 do
                local remaining = redis.call('HINCRBY', KEYS[2], values[i], -tonumber(values[i + 1]))
                if remaining <= 0 then
                    redis.call('HDEL', KEYS[2], values[i])
                end
            end
            redis.call('DEL', KEYS[1])
            redis.call('LREM', KEYS[3], 1, ARGV[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final PostMapper postMapper;
    private final ViewCountBatchWriter batchWriter;

    @Override
    @Async("viewCountExecutor")
    public void incrementViewCountAsync(Long postId, String userId) {
        if (postId == null) {
            return;
        }
        try {
            boolean authenticated = userId != null && !userId.isBlank();
            String dedupeKey = authenticated
                    ? DEDUPE_PREFIX + postId + ":" + userId
                    : ANONYMOUS_PLACEHOLDER;
            Long incremented = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(dedupeKey, ACTIVE_KEY, UNFLUSHED_KEY),
                    postId.toString(), authenticated ? "1" : "0", "86400");
            if (Long.valueOf(1L).equals(incremented)) {
                log.debug("帖子 {} 浏览量 +1", postId);
            }
        } catch (Exception e) {
            // 浏览量不是核心事实，统计失败不能影响帖子详情请求。
            log.warn("增加浏览量失败，postId: {}", postId, e);
        }
    }

    @Override
    public Long getViewCount(Long postId) {
        if (postId == null) {
            return 0L;
        }
        Post post = postMapper.selectById(postId);
        long dbCount = post == null || post.getViewCount() == null ? 0L : post.getViewCount();
        try {
            Object increment = redisTemplate.opsForHash().get(UNFLUSHED_KEY, postId.toString());
            return dbCount + (increment == null ? 0L : Long.parseLong(increment.toString()));
        } catch (Exception e) {
            log.warn("读取实时浏览量失败，返回数据库值，postId: {}", postId, e);
            return dbCount;
        }
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    public void syncViewCountToDb() {
        rotateActiveBatch();
        processPendingBatches();
    }

    private void rotateActiveBatch() {
        String batchId = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.execute(
                    ROTATE_SCRIPT,
                    List.of(ACTIVE_KEY, BATCH_PREFIX + batchId, PENDING_KEY),
                    batchId);
        } catch (Exception e) {
            log.warn("生成浏览量批次失败", e);
        }
    }

    private void processPendingBatches() {
        List<String> batchIds;
        try {
            batchIds = redisTemplate.opsForList().range(PENDING_KEY, 0, MAX_BATCHES_PER_RUN - 1);
        } catch (Exception e) {
            log.warn("读取浏览量待处理批次失败", e);
            return;
        }
        if (batchIds == null || batchIds.isEmpty()) {
            return;
        }

        for (String batchId : batchIds) {
            String batchKey = BATCH_PREFIX + batchId;
            try {
                Map<Object, Object> rawValues = redisTemplate.opsForHash().entries(batchKey);
                if (rawValues.isEmpty()) {
                    redisTemplate.opsForList().remove(PENDING_KEY, 1, batchId);
                    continue;
                }

                Map<String, Long> increments = new LinkedHashMap<>();
                rawValues.forEach((postId, delta) ->
                        increments.put(postId.toString(), Long.parseLong(delta.toString())));
                batchWriter.apply(batchId, increments);

                redisTemplate.execute(
                        ACK_SCRIPT,
                        List.of(batchKey, UNFLUSHED_KEY, PENDING_KEY),
                        batchId);
                log.info("浏览量批次同步完成，batchId: {}, postCount: {}", batchId, increments.size());
            } catch (Exception e) {
                log.error("浏览量批次同步失败，保留批次等待重试，batchId: {}", batchId, e);
                break;
            }
        }
    }
}
