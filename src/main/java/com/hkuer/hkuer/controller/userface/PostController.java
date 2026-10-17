package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.annotation.CheckMute;
import com.hkuer.hkuer.annotation.ViewCount;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.PostAppealSubmitRequest;
import com.hkuer.hkuer.entity.DTO.PostInsertRequest;
import com.hkuer.hkuer.service.userface.PostAppealService;
import com.hkuer.hkuer.service.userface.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post")
public class PostController {
    @Autowired
    private PostService postService;

    @Autowired
    private PostAppealService postAppealService;

    @PostMapping("/insert")
    @CheckMute(message = "您已被禁言，无法发布帖子")
    public Result insert(@RequestBody PostInsertRequest postInsertRequest) {
        Result result = postService.insertPost(postInsertRequest);
        return result;
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Long postId) {
        Result result = postService.deletePost(postId);
        return result;
    }

    @GetMapping("/page")
    public Result getPostPage(@RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(required = false) String boardName,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long postId) {
        Result result = postService.getPostPage(current, size, boardName, title, postId);
        return result;
    }

    @GetMapping("/liked-by-user")
    public Result getLikedPostPage(@RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return postService.getLikedPostPage(current, size, keyword);
    }

    @GetMapping("/favorited-by-user")
    public Result getFavoritedPostPage(@RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return postService.getFavoritedPostPage(current, size, keyword);
    }

    @GetMapping("/latest")
    public Result getLatestPosts() {
        return postService.getLatestPosts();
    }

    @GetMapping("/my-posts")
    public Result getMyPosts(@RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title) {
        return postService.getMyPosts(current, size, title);
    }

    /**
     * 获取帖子详情（会统计浏览量）
     * 使用 @ViewCount 注解，AOP 异步统计浏览量
     */
    @GetMapping("/detail")
    @ViewCount(postIdParam = "postId")
    public Result getPostDetail(@RequestParam Long postId) {
        return postService.getPostDetail(postId);
    }

    /**
     * 获取置顶帖子列表
     * 查询条件：status=1 且 is_top=1，按 top_time 倒序排列
     * 只返回3天内的置顶帖子（超过3天的自动过滤）
     */
    @GetMapping("/top")
    public Result getTopPosts() {
        return postService.getTopPosts();
    }

    /**
     * 切换帖子置顶状态
     * 普通状态(0) -> 置顶(1)，设置 top_time 为当前时间
     * 置顶状态(1) -> 取消置顶(0)，清空 top_time
     * 注意：超过3天的置顶帖子在查询时会自动过滤，无需手动取消
     *
     * @param postId 帖子ID
     * @return 操作结果
     */
    @PostMapping("/toggle-top")
    public Result toggleTop(@RequestParam Long postId) {
        return postService.toggleTop(postId);
    }

    /**
     * 提交帖子申诉
     *
     * @param request 申诉请求（包含 postId 和 reason）
     * @return 操作结果
     */
    @PostMapping("/appeal/submit")
    public Result submitAppeal(@RequestBody PostAppealSubmitRequest request) {
        return postAppealService.submitAppeal(request);
    }

}
