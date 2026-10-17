package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "post_like")
@Data
public class Like implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.INPUT)
    private Long likeId;
    private String userId;
    private Long targetId;
    private LocalDateTime createdAt;
}
