package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PostResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private String boardName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private Boolean isLiked; // 当前用户是否已点赞
    private Long likedNum;
    private Boolean isFavorited; // 当前用户是否已收藏
    private Long favoritedNum;
    private Long viewCount; // 浏览量
    private String authorNickname; // 作者昵称
    private String authorAvatar; // 作者头像
}
