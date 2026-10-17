package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.ReportHandleRequest;
import com.hkuer.hkuer.entity.DTO.ReportInsertRequest;
import com.hkuer.hkuer.entity.PO.CommentArticle;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.entity.PO.Report;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.entity.VO.ReportResponse;
import com.hkuer.hkuer.entity.enums.ReportTargetType;
import com.hkuer.hkuer.mapper.userface.CommentArticleMapper;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.mapper.userface.ReportMapper;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.ReportService;
import com.hkuer.hkuer.utils.SnowflakeIdGenerator;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 举报服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {
    @org.springframework.beans.factory.annotation.Autowired
    private SnowflakeIdGenerator idGenerator;

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentArticleMapper commentArticleMapper;
    private final UserMapper userMapper;
    private final com.hkuer.hkuer.mapper.userface.MeshReportMapper meshReportMapper;

    @Override
    public Result submitReport(ReportInsertRequest request) {
        // 1. 获取当前登录用户ID
        String reporterId = UserContext.getCurrentUserId();
        if (reporterId == null || reporterId.trim().isEmpty()) {
            return Result.error("401", "请先登录");
        }

        // 2. 参数校验
        if (request.getTargetId() == null || request.getTargetId().trim().isEmpty()) {
            return Result.error("400", "举报目标ID不能为空");
        }

        if (request.getTargetType() == null || request.getTargetType().trim().isEmpty()) {
            return Result.error("400", "举报目标类型不能为空");
        }

        ReportTargetType targetTypeEnum = ReportTargetType.fromCode(request.getTargetType());
        if (targetTypeEnum == null) {
            return Result.error("400", "举报目标类型不合法，支持: " + ReportTargetType.getAllCodes());
        }

        String targetType = targetTypeEnum.getCode();

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            return Result.error("400", "举报原因不能为空");
        }

        if(request.getReason().length()>500)return Result.error("400","举报原因最多 500 字");

        // 3. 防重复提交：检查是否已有待处理的举报记录
        QueryWrapper<Report> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("reporter_id", reporterId)
                .eq("target_id", request.getTargetId())
                .eq("target_type", targetType)
                .eq("status", 0); // 0-待处理

        Long existCount = reportMapper.selectCount(checkWrapper);
        if (existCount > 0) {
            return Result.error("400", "您已举报过该内容，请耐心等待处理");
        }

        // 4. 查询被举报目标的作者ID
        String targetUserId = getTargetUserId(request.getTargetId(), targetTypeEnum);
        if (targetUserId == null) {
            return Result.error("404", "举报目标不存在");
        }

        // 5. 不能举报自己
        if (reporterId.equals(targetUserId)) {
            return Result.error("400", "不能举报自己的内容");
        }

        // 6. 使用雪花算法生成举报ID

        Long reportId = idGenerator.nextId();

        // 7. 创建举报记录
        Report report = Report.builder()
                .id(reportId)
                .reporterId(reporterId)
                .targetId(request.getTargetId())
                .targetType(targetType)
                .targetUserId(targetUserId)
                .reason(request.getReason())
                .reasonDetail(request.getReasonDetail())
                .status(0) // 0-待处理
                .createTime(LocalDateTime.now())
                .build();

        reportMapper.insert(report);

        log.info("用户 {} 举报了 {} (类型: {})，举报ID: {}", reporterId, request.getTargetId(), targetType, report.getId());

        return Result.success("举报提交成功，我们会尽快处理");
    }

    @Override
    public Long getReportCount() {
        return reportMapper.selectCount(null);
    }

    @Override
    public Long getPendingReportCount() {
        QueryWrapper<Report> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0); // 0-待处理
        return reportMapper.selectCount(wrapper);
    }

    @Override
    public Result handleReport(ReportHandleRequest request) {
        // 1. 获取当前登录管理员ID
        String handlerId = UserContext.getCurrentUserId();
        if (handlerId == null || handlerId.trim().isEmpty()) {
            return Result.error("401", "请先登录");
        }

        // 2. 参数校验
        if (request.getReportId() == null || request.getReportId().trim().isEmpty()) {
            return Result.error("400", "举报ID不能为空");
        }

        if (request.getStatus() == null) {
            return Result.error("400", "处理状态不能为空");
        }

        // 3. 验证状态值（只能是1-已处罚/有效 或 2-已驳回/无效）
        if (request.getStatus() != 1 && request.getStatus() != 2) {
            return Result.error("400", "处理状态不合法，只能是1（已处罚/有效）或2（已驳回/无效）");
        }

        // 4. 将字符串ID转换为Long
        Long reportIdLong;
        try {
            reportIdLong = Long.valueOf(request.getReportId());
        } catch (NumberFormatException e) {
            return Result.error("400", "举报ID格式不正确");
        }

        // 5. 查询举报记录
        Report report = reportMapper.selectById(reportIdLong);
        if (report == null) {
            return Result.error("404", "举报记录不存在");
        }

        // 6. 检查是否已经处理过
        if (report.getStatus() != 0) {
            return Result.error("400", "该举报已被处理，无法重复处理");
        }

        // 7. 更新举报记录
        UpdateWrapper<Report> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", reportIdLong)
                .set("status", request.getStatus())
                .set("handler_id", handlerId)
                .set("handle_time", LocalDateTime.now())
                .set("handle_note", request.getHandleNote());

        int updateCount = reportMapper.update(null, updateWrapper);
        if (updateCount > 0) {
            String statusText = request.getStatus() == 1 ? "已处罚/有效" : "已驳回/无效";
            log.info("管理员 {} 处理了举报 {}，处理结果: {}", handlerId, reportIdLong, statusText);
            return Result.success("处理成功");
        } else {
            return Result.error("500", "处理失败，请重试");
        }
    }

    @Override
    public Map<String, Object> getPendingReports(Integer page, Integer size) {
        // 参数校验
        if (page == null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 10;

        // 查询条件：status=0（待处理）
        QueryWrapper<Report> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0);

        // 按创建时间倒序排列（最新的举报优先处理）
        queryWrapper.orderByDesc("create_time");

        // MyBatis-Plus 分页查询
        Page<Report> pageParam = new Page<>(page, size);
        Page<Report> pageResult = reportMapper.selectPage(pageParam, queryWrapper);

        // 转换为 VO
        List<ReportResponse> records = new ArrayList<>();

        for (Report report : pageResult.getRecords()) {
            ReportResponse response = new ReportResponse();
            response.setId(report.getId());
            response.setReporterId(report.getReporterId());
            response.setCreateTime(report.getCreateTime());
            response.setTargetId(report.getTargetId());
            response.setTargetType(report.getTargetType());
            response.setTargetUserId(report.getTargetUserId());
            response.setReason(report.getReason());
            response.setReasonDetail(report.getReasonDetail());
            response.setImages(report.getImages());
            response.setStatus(report.getStatus());
            response.setHandlerId(report.getHandlerId());
            response.setHandleTime(report.getHandleTime());
            response.setHandleNote(report.getHandleNote());

            // 查询举报人信息
            if (report.getReporterId() != null) {
                User reporter = userMapper.selectById(report.getReporterId());
                if (reporter != null) {
                    response.setReporterUsername(reporter.getUsername());
                    response.setReporterNickname(reporter.getNickname());
                }
            }

            // 查询被举报目标作者信息
            if (report.getTargetUserId() != null) {
                User targetUser = userMapper.selectById(report.getTargetUserId());
                if (targetUser != null) {
                    response.setTargetUsername(targetUser.getUsername());
                    response.setTargetNickname(targetUser.getNickname());
                }
            }

            records.add(response);
        }

        // 构建分页返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("pages", pageResult.getPages());

        return result;
    }

    /**
     * 根据目标ID和类型获取目标作者ID
     */
    private String getTargetUserId(String targetId, ReportTargetType targetType) {
        try {
            switch (targetType) {
                case POST:
                    Post post = postMapper.selectById(Long.valueOf(targetId));
                    return post != null ? post.getAuthorId() : null;

                case COMMENT:
                    CommentArticle comment = commentArticleMapper.selectById(Long.valueOf(targetId));
                    return comment != null ? comment.getUserId() : null;

                case ANSWER: return meshReportMapper.answer(Long.parseLong(targetId));
                case COURSE_REVIEW: return meshReportMapper.review(Long.parseLong(targetId));
                case COURSE_MATERIAL: return meshReportMapper.material(Long.parseLong(targetId));
                case USER:
                    User user = userMapper.selectById(targetId);
                    return user != null ? user.getId() : null;

                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            log.warn("目标ID格式错误: {}", targetId);
            return null;
        }
    }
}
