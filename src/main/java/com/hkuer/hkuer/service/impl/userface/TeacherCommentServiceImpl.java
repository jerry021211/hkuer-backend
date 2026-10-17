package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.TeacherCommentInsertRequest;
import com.hkuer.hkuer.entity.PO.TeacherComment;
import com.hkuer.hkuer.entity.PO.Teacher;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.entity.VO.TeacherCommentResponse;
import com.hkuer.hkuer.mapper.userface.TeacherCommentMapper;
import com.hkuer.hkuer.mapper.userface.TeacherMapper;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.TeacherCommentService;
import com.hkuer.hkuer.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户评论老师 - 实现
 */
@Slf4j
@Service
public class TeacherCommentServiceImpl extends ServiceImpl<TeacherCommentMapper, TeacherComment>
        implements TeacherCommentService {

    private static final int CONTENT_MAX_LENGTH = 2000;
    /** 状态：正常 */
    private static final int STATUS_NORMAL = 1;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result insert(TeacherCommentInsertRequest request) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("401", "未登录");
        }

        String teacherId = request != null ? request.getTeacherId() : null;
        String content = request != null ? request.getContent() : null;

        if (!StringUtils.hasText(teacherId)) {
            return Result.error("400", "教师ID不能为空");
        }
        if (!StringUtils.hasText(content)) {
            return Result.error("400", "评论内容不能为空");
        }
        content = content.trim();
        if (content.length() > CONTENT_MAX_LENGTH) {
            return Result.error("400", "评论内容长度不能超过" + CONTENT_MAX_LENGTH + "字");
        }

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return Result.error("400", "教师不存在");
        }

        TeacherComment comment = new TeacherComment();
        comment.setUserId(userId);
        comment.setTeacherId(teacherId);
        comment.setContent(content);
        comment.setStatus(STATUS_NORMAL);
        comment.setCreatedAt(LocalDateTime.now());

        save(comment);
        return Result.success("评论成功");
    }

    @Override
    public Result pageByTeacherId(String teacherId, Integer current, Integer size) {
        if (!StringUtils.hasText(teacherId)) {
            return Result.error("400", "教师ID不能为空");
        }
        if (current == null || current < 1) {
            current = 1;
        }
        if (size == null || size < 1 || size > 100) {
            size = 10;
        }

        Page<TeacherComment> pageParam = new Page<>(current, size);
        LambdaQueryWrapper<TeacherComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherComment::getTeacherId, teacherId)
                .orderByDesc(TeacherComment::getCreatedAt);

        IPage<TeacherComment> pageResult = page(pageParam, wrapper);

        List<TeacherComment> records = pageResult.getRecords();
        if (records == null || records.isEmpty()) {
            Page<TeacherCommentResponse> emptyPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(),
                    pageResult.getTotal());
            emptyPage.setRecords(new ArrayList<>());
            return Result.success(emptyPage);
        }

        // 批量查询用户信息，避免 N+1
        Set<String> userIds = records.stream()
                .map(TeacherComment::getUserId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            if (users != null) {
                for (User u : users) {
                    if (u != null && u.getId() != null) {
                        userMap.put(u.getId(), u);
                    }
                }
            }
        }

        List<TeacherCommentResponse> voList = new ArrayList<>(records.size());
        for (TeacherComment c : records) {
            TeacherCommentResponse vo = new TeacherCommentResponse();
            vo.setId(c.getId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setCreatedAt(c.getCreatedAt());

            User u = userMap.get(c.getUserId());
            if (u != null) {
                vo.setNickname(u.getNickname());
                vo.setAvatar(u.getAvatar());
            }
            voList.add(vo);
        }

        Page<TeacherCommentResponse> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(),
                pageResult.getTotal());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }
}
