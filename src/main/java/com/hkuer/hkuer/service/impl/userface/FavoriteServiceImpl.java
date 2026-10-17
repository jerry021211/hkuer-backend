package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.PO.Favorite;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.mapper.userface.FavoriteMapper;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.messaging.InteractionCacheService;
import com.hkuer.hkuer.messaging.InteractionEventTypes;
import com.hkuer.hkuer.messaging.InteractionOutboxService;
import com.hkuer.hkuer.service.userface.FavoriteService;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final PostMapper postMapper;
    private final InteractionCacheService cacheService;
    private final InteractionOutboxService outboxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result favorite(Long targetId) {
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

        Favorite favorite = new Favorite();
        favorite.setFavoriteId(IdWorker.getId());
        favorite.setUserId(userId);
        favorite.setTargetId(targetId);
        favorite.setCreatedAt(LocalDateTime.now().withNano(0));

        int inserted = favoriteMapper.insertIgnore(favorite);
        long count = post.getFavNum() == null ? 0L : post.getFavNum();
        if (inserted > 0) {
            if (postMapper.incrementFavoriteCount(targetId, 1) != 1) {
                throw new IllegalStateException("更新收藏数失败");
            }
            count++;
            outboxService.append(InteractionEventTypes.POST_FAVORITE_CHANGED, "POST", targetId.toString(), userId);
        }
        return Result.success(count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result unfavorite(Long targetId) {
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

        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetId, targetId));
        long count = post.getFavNum() == null ? 0L : post.getFavNum();
        if (deleted > 0) {
            if (postMapper.incrementFavoriteCount(targetId, -1) != 1) {
                throw new IllegalStateException("更新收藏数失败");
            }
            count = Math.max(0L, count - 1);
            outboxService.append(InteractionEventTypes.POST_FAVORITE_CHANGED, "POST", targetId.toString(), userId);
        }
        return Result.success(count);
    }

    @Override
    public Result isFavorited(Long targetId) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("401", "未登录");
        }
        if (targetId == null) {
            return Result.error("400", "targetId不能为空");
        }
        return Result.success(cacheService.isFavorited(userId, targetId));
    }
}
