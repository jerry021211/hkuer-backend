package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户对教师的评论
 */
@Data
@TableName(value = "course_comment")
public class TeacherComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String userId;

    private String teacherId;

    private String content;

    /** 状态：0-待审核 1-正常 2-隐藏等 */
    private Integer status;

    private LocalDateTime createdAt;
}
