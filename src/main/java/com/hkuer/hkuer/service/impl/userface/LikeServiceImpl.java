package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.PO.Like;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.mapper.userface.LikeMapper;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.messaging.InteractionCacheService;
import com.hkuer.hkuer.messaging.InteractionEventTypes;
import com.hkuer.hkuer.messaging.InteractionOutboxService;
import com.hkuer.hkuer.service.userface.LikeService;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private MeshNotificationService meshNotifications;
    private final LikeMapper likeMapper;
    private final PostMapper postMapper;
    private final InteractionCacheService cacheService;
    private final InteractionOutboxService outboxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result like(Long targetId) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("401", "未登录");
        }
        if (targetId == null) {
            return Result.error("400", "targetId不能为空");
        }

        Post post = postMapper.selectByIdForUpdate(targetId);
        if (post == null) {
            return Result.error("404", "帖子不存在");
        }

        Like like = new Like();
        like.setLikeId(IdWorker.getId());
        like.setUserId(userId);
        like.setTargetId(targetId);
        like.setCreatedAt(LocalDateTime.now().withNano(0));

        int inserted = likeMapper.insertIgnore(like);
        long count = post.getLikeNum() == null ? 0L : post.getLikeNum();
        if (inserted > 0) {
            if (postMapper.incrementLikeCount(targetId, 1) != 1) {
                throw new IllegalStateException("更新点赞数失败");
            }
            count++;
            if(meshNotifications!=null)meshNotifications.post(targetId,userId,"like","有人赞了你的内容",null);
            outboxService.append(InteractionEventTypes.POST_LIKE_CHANGED, "POST", targetId.toString(), userId);
        }
        return Result.success(count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result unlike(Long targetId) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("401", "未登录");
        }
        if (targetId == null) {
            return Result.error("400", "targetId不能为空");
        }

        Post post = postMapper.selectByIdForUpdate(targetId);
        if (post == null) {
            return Result.error("404", "帖子不存在");
        }

        int deleted = likeMapper.delete(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetId, targetId));
        long count = post.getLikeNum() == null ? 0L : post.getLikeNum();
        if (deleted > 0) {
            if (postMapper.incrementLikeCount(targetId, -1) != 1) {
                throw new IllegalStateException("更新点赞数失败");
            }
            count = Math.max(0L, count - 1);
            outboxService.append(InteractionEventTypes.POST_LIKE_CHANGED, "POST", targetId.toString(), userId);
        }
        return Result.success(count);
    }

    @Override
    public Result isLiked(Long targetId) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("401", "未登录");
        }
        if (targetId == null) {
            return Result.error("400", "targetId不能为空");
        }
        return Result.success(cacheService.isLiked(userId, targetId));
    }
}
