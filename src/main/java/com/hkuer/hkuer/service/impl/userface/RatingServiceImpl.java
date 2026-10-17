package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.PO.Rating;
import com.hkuer.hkuer.entity.PO.Teacher;
import com.hkuer.hkuer.mapper.userface.RatingMapper;
import com.hkuer.hkuer.mapper.userface.TeacherMapper;
import com.hkuer.hkuer.messaging.InteractionCacheService;
import com.hkuer.hkuer.messaging.InteractionEventTypes;
import com.hkuer.hkuer.messaging.InteractionOutboxService;
import com.hkuer.hkuer.service.userface.RatingService;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingServiceImpl extends ServiceImpl<RatingMapper, Rating> implements RatingService {

    private final TeacherMapper teacherMapper;
    private final InteractionCacheService cacheService;
    private final InteractionOutboxService outboxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result rateTeacher(String teacherId, Integer newScore) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("401", "未登录");
        }
        if (teacherId == null || teacherId.isBlank() || newScore == null || newScore < 1 || newScore > 5) {
            return Result.error("400", "评分参数不正确");
        }

        Teacher teacher = teacherMapper.selectByIdForUpdate(teacherId);
        if (teacher == null) {
            return Result.error("404", "教师不存在");
        }

        Rating rating = baseMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getTargetId, teacherId)
                .last("LIMIT 1"));
        if (rating != null && newScore.equals(rating.getScore())) {
            return Result.success("您的分数已经修改");
        }

        long totalScore = teacher.getTotalScore() == null ? 0L : teacher.getTotalScore();
        int ratingCount = teacher.getRatingCount() == null ? 0 : teacher.getRatingCount();
        LocalDateTime now = LocalDateTime.now().withNano(0);

        if (rating == null) {
            rating = new Rating();
            rating.setId(UUID.randomUUID().toString().replace("-", ""));
            rating.setUserId(userId);
            rating.setTargetId(teacherId);
            rating.setScore(newScore);
            rating.setCreatedAt(now);
            baseMapper.insert(rating);
            totalScore += newScore;
            ratingCount++;
        } else {
            totalScore += newScore - rating.getScore();
            rating.setScore(newScore);
            rating.setCreatedAt(now);
            baseMapper.updateById(rating);
        }

        Teacher update = new Teacher();
        update.setId(teacherId);
        update.setTotalScore(totalScore);
        update.setRatingCount(ratingCount);
        update.setAverageRating(calculateAverage(totalScore, ratingCount));
        if (teacherMapper.updateById(update) != 1) {
            throw new IllegalStateException("更新教师评分汇总失败");
        }

        outboxService.append(
                InteractionEventTypes.TEACHER_RATING_CHANGED, "TEACHER", teacherId, userId);
        return Result.success("分数加入");
    }

    @Override
    public Teacher getTeacherStats(String teacherId) {
        Teacher teacher = cacheService.getTeacherStats(teacherId);
        String userId = UserContext.getCurrentUserId();
        if (teacher != null && userId != null) {
            teacher.setUserRating(cacheService.getUserRating(userId, teacherId));
        }
        return teacher;
    }

    @Override
    public Result getTeacherList(int current, int size, String keyword, Boolean isDesc) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("401", "未登录");
        }
        try {
            Page<Teacher> pageParam = new Page<>(current, size);
            LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StringUtils.hasText(keyword), Teacher::getName, keyword)
                    .orderBy(true, !isDesc, Teacher::getTotalScore);
            IPage<Teacher> pageResult = teacherMapper.selectTeacherListWithUserRating(
                    pageParam, wrapper, userId);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("分页查询失败", e);
            return Result.error("500", "获取列表失败");
        }
    }

    private BigDecimal calculateAverage(long totalScore, int ratingCount) {
        if (ratingCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalScore)
                .divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
    }
}
