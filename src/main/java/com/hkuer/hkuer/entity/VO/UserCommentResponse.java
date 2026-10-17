package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCommentResponse {
    private Long postId;
    private String content;
    private LocalDateTime createdAt;
}




