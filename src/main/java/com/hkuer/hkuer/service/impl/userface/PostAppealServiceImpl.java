package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.PostAppealAuditRequest;
import com.hkuer.hkuer.entity.DTO.PostAppealSubmitRequest;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.entity.PO.PostAppeal;
import com.hkuer.hkuer.mapper.userface.PostAppealMapper;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.service.userface.PostAppealService;
import com.hkuer.hkuer.utils.SnowflakeIdGenerator;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子申诉服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostAppealServiceImpl implements PostAppealService {
    @org.springframework.beans.factory.annotation.Autowired
    private SnowflakeIdGenerator idGenerator;

    private final PostAppealMapper postAppealMapper;
    private final PostMapper postMapper;

    @Override
    @Transactional
    public Result submitAppeal(PostAppealSubmitRequest request) {
        // 1. 获取当前登录用户ID
        String currentUserIdStr = UserContext.getCurrentUserId();
        if (currentUserIdStr == null || currentUserIdStr.trim().isEmpty()) {
            return Result.error("401", "请先登录");
        }

        // 2. 参数校验
        if (request.getPostId() == null) {
            return Result.error("400", "帖子ID不能为空");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            return Result.error("400", "申诉理由不能为空");
        }

        // 3. 查询帖子是否存在
        Post post = postMapper.selectById(request.getPostId());
        if (post == null) {
            return Result.error("404", "帖子不存在");
        }

        // 4. 校验帖子是否属于当前登录用户
        if (!currentUserIdStr.equals(post.getAuthorId())) {
            return Result.error("403", "只能申诉自己发布的帖子");
        }

        // 5. 校验帖子状态必须为 3（异常状态：被系统拦截或申诉中）
        if (post.getStatus() == null || post.getStatus() != 3) {
            return Result.error("400", "只有状态为异常（被拦截）的帖子才能申诉");
        }

        // 6. 检查是否已有待处理的申诉记录（防重复提交）
        LambdaQueryWrapper<PostAppeal> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(PostAppeal::getPostId, request.getPostId())
                .eq(PostAppeal::getUserId, currentUserIdStr)
                .eq(PostAppeal::getStatus, 0); // 0-待处理

        Long existCount = postAppealMapper.selectCount(checkWrapper);
        if (existCount > 0) {
            return Result.error("400", "您已提交过申诉，请耐心等待审核");
        }

        // 7. 使用雪花算法生成申诉ID

        Long appealId = idGenerator.nextId();

        // 8. 创建申诉记录
        PostAppeal appeal = new PostAppeal();
        appeal.setId(appealId);
        appeal.setPostId(request.getPostId());
        appeal.setUserId(currentUserIdStr);
        appeal.setReason(request.getReason().trim());
        appeal.setStatus(0); // 0-待处理
        appeal.setCreateTime(LocalDateTime.now());

        postAppealMapper.insert(appeal);

        log.info("用户 {} 对帖子 {} 提交了申诉，申诉ID: {}", currentUserIdStr, request.getPostId(), appeal.getId());

        return Result.success("申诉已提交，请等待人工审核");
    }

    @Override
    @Transactional
    public Result auditAppeal(PostAppealAuditRequest request) {
        // 1. 获取当前登录管理员ID
        String adminId = UserContext.getCurrentUserId();
        if (adminId == null || adminId.trim().isEmpty()) {
            return Result.error("401", "请先登录");
        }

        // 2. 参数校验
        if (request.getAppealId() == null) {
            return Result.error("400", "申诉ID不能为空");
        }

        if (request.getPass() == null) {
            return Result.error("400", "审核结果不能为空");
        }

        // 3. 如果驳回，必须提供驳回原因
        if (!request.getPass() && (request.getReply() == null || request.getReply().trim().isEmpty())) {
            return Result.error("400", "驳回时必须填写驳回原因");
        }

        // 4. 查询申诉记录
        PostAppeal appeal = postAppealMapper.selectById(request.getAppealId());
        if (appeal == null) {
            return Result.error("404", "申诉记录不存在");
        }

        // 5. 检查是否已经处理过
        if (appeal.getStatus() != 0) {
            return Result.error("400", "该申诉已被处理，无法重复处理");
        }

        // 6. 查询关联的帖子
        Post post = postMapper.selectById(appeal.getPostId());
        if (post == null) {
            return Result.error("404", "关联的帖子不存在");
        }

        // 7. 根据审核结果处理
        LocalDateTime now = LocalDateTime.now();

        if (request.getPass()) {
            // 通过：将帖子状态更新为 1（正常发布），申诉状态更新为 1（通过）
            LambdaUpdateWrapper<Post> postUpdateWrapper = new LambdaUpdateWrapper<>();
            postUpdateWrapper.eq(Post::getId, appeal.getPostId())
                    .set(Post::getStatus, 1); // 1-正常发布

            postMapper.update(null, postUpdateWrapper);

            LambdaUpdateWrapper<PostAppeal> appealUpdateWrapper = new LambdaUpdateWrapper<>();
            appealUpdateWrapper.eq(PostAppeal::getId, request.getAppealId())
                    .set(PostAppeal::getStatus, 1) // 1-通过
                    .set(PostAppeal::getHandleTime, now);

            postAppealMapper.update(null, appealUpdateWrapper);

            log.info("管理员 {} 审核通过申诉 {}，帖子 {} 已恢复为正常状态", adminId, request.getAppealId(), appeal.getPostId());

            return Result.success("申诉已通过，帖子已恢复为正常状态");

        } else {
            // 驳回：帖子状态保持为 3 不变，申诉状态更新为 2（驳回），记录驳回原因
            LambdaUpdateWrapper<PostAppeal> appealUpdateWrapper = new LambdaUpdateWrapper<>();
            appealUpdateWrapper.eq(PostAppeal::getId, request.getAppealId())
                    .set(PostAppeal::getStatus, 2) // 2-驳回
                    .set(PostAppeal::getAdminReply, request.getReply().trim())
                    .set(PostAppeal::getHandleTime, now);

            postAppealMapper.update(null, appealUpdateWrapper);

            log.info("管理员 {} 驳回了申诉 {}，驳回原因: {}", adminId, request.getAppealId(), request.getReply());

            return Result.success("申诉已驳回");
        }
    }

    /**
     * 获取所有待处理的申诉列表（status = 0）
     *
     * @return 待处理申诉列表
     */
    @Override
    public Result getPendingAppeals() {
        LambdaQueryWrapper<PostAppeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostAppeal::getStatus, 0)
                .orderByDesc(PostAppeal::getCreateTime);

        List<PostAppeal> pendingAppeals = postAppealMapper.selectList(wrapper);
        return Result.success(pendingAppeals);
    }

    @Override
    public Long getAppealCount() {
        return postAppealMapper.selectCount(null);
    }

    @Override
    public Long getPendingAppealCount() {
        LambdaQueryWrapper<PostAppeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostAppeal::getStatus, 0);
        return postAppealMapper.selectCount(wrapper);
    }
}
