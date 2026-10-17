package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value ="comment_article")
@Data
public class CommentArticle implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.INPUT)
    private Long callId;
    private Long postId;
    private String userId;
    private String contentMd;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
