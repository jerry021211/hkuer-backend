package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.io.Serializable;

@Data
public class LikedPostResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long postId;
    private String title;
    private String content;
}
