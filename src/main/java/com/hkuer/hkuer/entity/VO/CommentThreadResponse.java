package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentThreadResponse {
    private Long callId;      // 线程根ID
    private Long postId;
    private String userId;
    private String nickname;
    private String contentMd;
    private LocalDateTime createdAt;
    private List<ReplyResponse> replies;
}




