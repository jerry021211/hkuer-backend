package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 附件（图片）表实体
 * status: 0-临时(PENDING), 1-正式(ACTIVE)
 */
@TableName("attachment")
@Data
public class Attachment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件完整 URL */
    private String url;

    /** 状态：0-临时，1-正式 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
