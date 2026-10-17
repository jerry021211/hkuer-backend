package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "post")
@Data
public class Post implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long boardId;
    private String authorId;
    private String title;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private Long likeNum = 0L;
    private Long favNum = 0L;
    private Long viewCount = 0L;  // 浏览量
    /** 是否置顶: 0-否, 1-是 */
    private Integer isTop;
    /** 置顶操作时间 (进阶版核心) */
    private LocalDateTime topTime;
}
