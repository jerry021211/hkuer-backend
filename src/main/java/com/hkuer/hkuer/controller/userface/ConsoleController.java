package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.PostAppealAuditRequest;
import com.hkuer.hkuer.entity.DTO.ReportHandleRequest;
import com.hkuer.hkuer.service.userface.BanService;
import com.hkuer.hkuer.service.userface.CommentService;
import com.hkuer.hkuer.service.userface.MuteService;
import com.hkuer.hkuer.service.userface.PostAppealService;
import com.hkuer.hkuer.service.userface.PostService;
import com.hkuer.hkuer.service.userface.ReportService;
import com.hkuer.hkuer.service.userface.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理接口控制器
 * 所有接口以 /console 开头
 */
@RestController
@RequestMapping("/console")
@RequiredArgsConstructor
public class ConsoleController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final ReportService reportService;
    private final MuteService muteService;
    private final BanService banService;
    private final PostAppealService postAppealService;

    /**
     * 获取用户总数
     */
    @GetMapping("/user/count")
    public Result getUserCount() {
        try {
            Long count = userService.getUserCount();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("500", "获取用户数量失败");
        }
    }

    /**
     * 获取所有用户信息列表（分页，不包括管理员）
     * 
     * @param page     页码（从1开始，默认1）
     * @param size     每页数量（默认10）
     * @param email    邮箱关键词（可选，模糊匹配）
     * @param nickname 昵称关键词（可选，模糊匹配）
     */
    @GetMapping("/user/list")
    public Result getAllUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname) {
        try {
            return Result.success(userService.getAllUsers(page, size, email, nickname));
        } catch (Exception e) {
            return Result.error("500", "获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有用户信息列表（分页，包括管理员）
     * 
     * @param page     页码（从1开始，默认1）
     * @param size     每页数量（默认10）
     * @param email    邮箱关键词（可选，模糊匹配）
     * @param nickname 昵称关键词（可选，模糊匹配）
     */
    @GetMapping("/user/all-list")
    public Result getAllUsersIncludingAdmin(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname) {
        try {
            return Result.success(userService.getAllUsersIncludingAdmin(page, size, email, nickname));
        } catch (Exception e) {
            return Result.error("500", "获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 更改用户权限
     *
     * @param userId   用户ID
     * @param usertype 用户类型：0-取消管理员，1-设置管理员
     */
    @PostMapping("/user/update-usertype")
    public Result updateUserType(@RequestParam String userId, @RequestParam Integer usertype) {
        return userService.updateUserType(userId, usertype);
    }

    /**
     * 获取帖子总数
     */
    @GetMapping("/post/count")
    public Result getPostCount() {
        try {
            Long count = postService.getPostCount();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("500", "获取帖子数量失败");
        }
    }

    /**
     * 获取评论总数（包括根评论和回复评论）
     */
    @GetMapping("/comment/count")
    public Result getCommentCount() {
        try {
            Long count = commentService.getCommentCount();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("500", "获取评论数量失败");
        }
    }

    // ==================== 举报管理接口 ====================

    /**
     * 获取举报总数
     */
    @GetMapping("/report/count")
    public Result getReportCount() {
        try {
            Long count = reportService.getReportCount();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("500", "获取举报数量失败");
        }
    }

    /**
     * 获取待处理举报数量
     */
    @GetMapping("/report/pending-count")
    public Result getPendingReportCount() {
        try {
            Long count = reportService.getPendingReportCount();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("500", "获取待处理举报数量失败");
        }
    }

    /**
     * 分页获取待处理举报列表（status=0）
     *
     * @param page 页码（从1开始，默认1）
     * @param size 每页数量（默认10）
     */
    @GetMapping("/report/pending-list")
    public Result getPendingReports(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            return Result.success(reportService.getPendingReports(page, size));
        } catch (Exception e) {
            return Result.error("500", "获取待处理举报列表失败: " + e.getMessage());
        }
    }

    /**
     * 处理举报
     *
     * @param request 处理请求
     */
    @PostMapping("/report/handle")
    public Result handleReport(@RequestBody ReportHandleRequest request) {
        return reportService.handleReport(request);
    }

    // ==================== 禁言管理接口 ====================

    /**
     * 禁言用户
     *
     * @param userId 用户ID
     * @param days   禁言天数
     */
    @PostMapping("/user/mute")
    public Result muteUser(@RequestParam String userId, @RequestParam int days) {
        return muteService.muteUser(userId, days);
    }

    /**
     * 解除禁言
     *
     * @param userId 用户ID
     */
    @PostMapping("/user/unmute")
    public Result unmuteUser(@RequestParam String userId) {
        return muteService.unmuteUser(userId);
    }

    /**
     * 检查用户禁言状态
     *
     * @param userId 用户ID
     */
    @GetMapping("/user/mute-status")
    public Result getMuteStatus(@RequestParam String userId) {
        boolean isMuted = muteService.isMuted(userId);
        if (isMuted) {
            long remainingMinutes = muteService.getMuteRemainingMinutes(userId);
            return Result.success(new MuteStatusResponse(true, remainingMinutes));
        }
        return Result.success(new MuteStatusResponse(false, 0));
    }

    /**
     * 获取所有被禁言的用户列表
     */
    @GetMapping("/user/muted-list")
    public Result getMutedUsers() {
        try {
            return Result.success(muteService.getMutedUsers());
        } catch (Exception e) {
            return Result.error("500", "获取被禁言用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 禁言状态响应内部类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MuteStatusResponse {
        private boolean muted;
        private long remainingMinutes;
    }

    // ==================== 封号管理接口 ====================

    /**
     * 封禁用户
     *
     * @param userId 用户ID
     * @param days   封禁天数
     */
    @PostMapping("/user/ban")
    public Result banUser(@RequestParam String userId, @RequestParam int days) {
        return banService.banUser(userId, days);
    }

    /**
     * 解除封禁
     *
     * @param userId 用户ID
     */
    @PostMapping("/user/unban")
    public Result unbanUser(@RequestParam String userId) {
        return banService.unbanUser(userId);
    }

    /**
     * 检查用户封号状态
     *
     * @param userId 用户ID
     */
    @GetMapping("/user/ban-status")
    public Result getBanStatus(@RequestParam String userId) {
        boolean isBanned = banService.isBanned(userId);
        if (isBanned) {
            long remainingMinutes = banService.getBanRemainingMinutes(userId);
            return Result.success(new BanStatusResponse(true, remainingMinutes));
        }
        return Result.success(new BanStatusResponse(false, 0));
    }

    /**
     * 获取所有被封号的用户列表
     */
    @GetMapping("/user/banned-list")
    public Result getBannedUsers() {
        try {
            return Result.success(banService.getBannedUsers());
        } catch (Exception e) {
            return Result.error("500", "获取被封号用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 封号状态响应内部类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class BanStatusResponse {
        private boolean banned;
        private long remainingMinutes;
    }

    // ==================== 申诉管理接口 ====================

    /**
     * 获取申诉总数
     */
    @GetMapping("/appeal/count")
    public Result getAppealCount() {
        try {
            Long count = postAppealService.getAppealCount();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("500", "获取申诉总数失败");
        }
    }

    /**
     * 获取申诉数量（status=0）
     */
    @GetMapping("/appeal/pending-count")
    public Result getPendingAppealCount() {
        try {
            Long count = postAppealService.getPendingAppealCount();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("500", "获取待处理申诉数量失败");
        }
    }

    /**
     * 获取所有待处理的申诉列表（status=0）
     *
     * @return 待处理申诉列表
     */
    @GetMapping("/appeal/pending-list")
    public Result getPendingAppeals() {
        return postAppealService.getPendingAppeals();
    }

    /**
     * 管理员审核申诉
     *
     * @param request 审核请求（包含 appealId, pass, reply）
     * @return 操作结果
     */
    @PostMapping("/appeal/audit")
    public Result auditAppeal(@RequestBody PostAppealAuditRequest request) {
        return postAppealService.auditAppeal(request);
    }
}
