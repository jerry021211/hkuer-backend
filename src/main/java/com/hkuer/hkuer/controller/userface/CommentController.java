package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.annotation.CheckMute;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.CommentArticleRequest;
import com.hkuer.hkuer.entity.DTO.CommentReplyRequest;
import com.hkuer.hkuer.service.userface.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/article")
    @CheckMute(message = "您已被禁言，无法发表评论")
    public Result addArticleComment(@RequestBody CommentArticleRequest request) {
        return commentService.addArticleComment(request);
    }

    @PostMapping("/reply")
    @CheckMute(message = "您已被禁言，无法回复评论")
    public Result addReply(@RequestBody CommentReplyRequest request) {
        return commentService.addReply(request);
    }

    @GetMapping("/list")
    public Result list(@RequestParam Long postId) {
        return commentService.listByPostId(postId);
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Long targetId) {
        return commentService.deleteComment(targetId);
    }

    @GetMapping("/by-user")
    public Result listByUser(@RequestParam(defaultValue = "1") Integer pageNo,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) String keyword) {
        return commentService.listByUserId(pageNo, pageSize, keyword);
    }

}


