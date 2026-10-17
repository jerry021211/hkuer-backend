package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReplyResponse {
    private Long id;
    private String fromUserId;
    private String fromNickname;
    private String toUserId;
    private String toNickname;
    private String contentMd;
    private LocalDateTime createdAt;
    private List<ReplyResponse> children;
}




