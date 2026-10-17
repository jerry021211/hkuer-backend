package com.hkuer.hkuer.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hkuer.hkuer.entity.PO.Favorite;
import com.hkuer.hkuer.entity.PO.Like;
import com.hkuer.hkuer.entity.PO.Rating;
import com.hkuer.hkuer.entity.PO.Teacher;
import com.hkuer.hkuer.mapper.userface.FavoriteMapper;
import com.hkuer.hkuer.mapper.userface.LikeMapper;
import com.hkuer.hkuer.mapper.userface.RatingMapper;
import com.hkuer.hkuer.mapper.userface.TeacherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class InteractionCacheService {

    private static final String LIKE_STATE = "hkuer:cache:like:state:";
    private static final String FAVORITE_STATE = "hkuer:cache:favorite:state:";
    private static final String RATING_STATS = "hkuer:cache:rating:stats:";
    private static final String USER_RATING = "hkuer:cache:rating:user:";
    private static final String NULL_VALUE = "-";

    private final StringRedisTemplate redisTemplate;
    private final LikeMapper likeMapper;
    private final FavoriteMapper favoriteMapper;
    private final TeacherMapper teacherMapper;
    private final RatingMapper ratingMapper;

    public boolean isLiked(String userId, Long postId) {
        String key = LIKE_STATE + userId + ":" + postId;
        String cached = getValue(key);
        if (cached != null) {
            return "1".equals(cached);
        }
        boolean liked = likeMapper.selectCount(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetId, postId)) > 0;
        putValue(key, liked ? "1" : "0", stateTtl());
        return liked;
    }

    public boolean isFavorited(String userId, Long postId) {
        String key = FAVORITE_STATE + userId + ":" + postId;
        String cached = getValue(key);
        if (cached != null) {
            return "1".equals(cached);
        }
        boolean favorited = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetId, postId)) > 0;
        putValue(key, favorited ? "1" : "0", stateTtl());
        return favorited;
    }

    public Teacher getTeacherStats(String teacherId) {
        String key = RATING_STATS + teacherId;
        try {
            Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
            if (cached.containsKey("totalScore") && cached.containsKey("ratingCount")) {
                Teacher teacher = new Teacher();
                teacher.setId(teacherId);
                teacher.setName(String.valueOf(cached.getOrDefault("name", "")));
                teacher.setTotalScore(Long.parseLong(cached.get("totalScore").toString()));
                teacher.setRatingCount(Integer.parseInt(cached.get("ratingCount").toString()));
                teacher.setAverageRating(calculateAverage(teacher.getTotalScore(), teacher.getRatingCount()));
                return teacher;
            }
        } catch (Exception e) {
            log.warn("读取教师评分缓存失败，回源数据库，teacherId: {}", teacherId, e);
        }

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher != null) {
            cacheTeacher(key, teacher);
        }
        return teacher;
    }

    public Integer getUserRating(String userId, String teacherId) {
        String key = USER_RATING + userId + ":" + teacherId;
        String cached = getValue(key);
        if (cached != null) {
            return NULL_VALUE.equals(cached) ? null : Integer.valueOf(cached);
        }
        Rating rating = ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getTargetId, teacherId)
                .last("LIMIT 1"));
        Integer score = rating == null ? null : rating.getScore();
        putValue(key, score == null ? NULL_VALUE : score.toString(), stateTtl());
        return score;
    }

    public void evict(InteractionEventMessage event) {
        String actorId = event.actorId();
        String aggregateId = event.aggregateId();
        switch (event.eventType()) {
            case InteractionEventTypes.POST_LIKE_CHANGED -> redisTemplate.delete(
                    LIKE_STATE + actorId + ":" + aggregateId);
            case InteractionEventTypes.POST_FAVORITE_CHANGED -> redisTemplate.delete(
                    FAVORITE_STATE + actorId + ":" + aggregateId);
            case InteractionEventTypes.TEACHER_RATING_CHANGED -> redisTemplate.delete(List.of(
                    RATING_STATS + aggregateId,
                    USER_RATING + actorId + ":" + aggregateId));
            default -> throw new IllegalArgumentException("无法处理的互动事件: " + event.eventType());
        }
    }

    private String getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取互动缓存失败，回源数据库，key: {}", key, e);
            return null;
        }
    }

    private void putValue(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("写入互动缓存失败，key: {}", key, e);
        }
    }

    private void cacheTeacher(String key, Teacher teacher) {
        try {
            Map<String, String> values = new HashMap<>();
            values.put("name", teacher.getName() == null ? "" : teacher.getName());
            values.put("totalScore", Long.toString(teacher.getTotalScore() == null ? 0L : teacher.getTotalScore()));
            values.put("ratingCount", Integer.toString(teacher.getRatingCount() == null ? 0 : teacher.getRatingCount()));
            redisTemplate.opsForHash().putAll(key, values);
            redisTemplate.expire(key, countTtl());
        } catch (Exception e) {
            log.warn("写入教师评分缓存失败，teacherId: {}", teacher.getId(), e);
        }
    }

    private BigDecimal calculateAverage(long totalScore, int ratingCount) {
        if (ratingCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalScore)
                .divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
    }

    private Duration countTtl() {
        return Duration.ofSeconds(600 + ThreadLocalRandom.current().nextInt(121));
    }

    private Duration stateTtl() {
        return Duration.ofSeconds(300 + ThreadLocalRandom.current().nextInt(61));
    }
}
