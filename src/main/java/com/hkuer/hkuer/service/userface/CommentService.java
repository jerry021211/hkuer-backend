package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.CommentArticleRequest;
import com.hkuer.hkuer.entity.DTO.CommentReplyRequest;
import com.hkuer.hkuer.entity.VO.UserCommentResponse;

public interface CommentService {
    Result addArticleComment(CommentArticleRequest request);

    Result addReply(CommentReplyRequest request);

    Result listByPostId(Long postId);

    Result deleteComment(Long targetId);

    Result listByUserId(Integer pageNo, Integer pageSize, String keyword);
    
    /**
     * 获取评论总数（包括根评论和回复评论）
     */
    Long getCommentCount();
}
