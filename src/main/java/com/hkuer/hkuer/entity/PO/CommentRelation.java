package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value ="comment_relation")
@Data
public class CommentRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.INPUT)
    private Long reId;
    private Long postId;
    private Long callId;
    private String reUserId;
    private String reComment;
    private Long parentReId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
