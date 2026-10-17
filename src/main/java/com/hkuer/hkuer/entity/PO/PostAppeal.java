package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子申诉记录实体类
 */
@TableName(value = "post_appeal")
@Data
public class PostAppeal implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 关联的帖子ID
     */
    private Long postId;

    /**
     * 申诉人ID
     */
    private String userId;

    /**
     * 申诉理由
     */
    private String reason;

    /**
     * 处理状态：0-待处理, 1-通过, 2-驳回
     */
    private Integer status;

    /**
     * 管理员回复(驳回原因)
     */
    private String adminReply;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
