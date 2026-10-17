package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "report", autoResultMap = true)
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 举报人ID
     */
    private String reporterId;

    /**
     * 举报提交时间
     */
    private LocalDateTime createTime;

    /**
     * 被举报目标ID（帖子ID、评论ID、用户ID）
     */
    private String targetId;

    /**
     * 目标类型: POST-帖子, COMMENT-评论, USER-用户
     */
    private String targetType;

    /**
     * 被举报内容的作者ID (用于统计某人被举报次数)
     */
    private String targetUserId;

    /**
     * 举报类型: 垃圾广告/违法违规/涉黄/人身攻击
     */
    private String reason;

    /**
     * 详细描述 (用户填写的补充说明)
     */
    private String reasonDetail;

    /**
     * 证据图片 (存图片URL数组)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /**
     * 举报状态: 0-待处理, 1-已处罚/有效, 2-已驳回/无效
     */
    private Integer status;

    /**
     * 处理管理员ID
     */
    private String handlerId;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理结果备注
     */
    private String handleNote;
}
