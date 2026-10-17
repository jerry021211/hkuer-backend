package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

@Data
public class CommentReplyRequest {
    private Long parentId;    // 被回复的id：可为callId或reId
    private String contentMd;
}




