package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.io.Serializable;

@Data
@TableName(value = "teacher")
public class Teacher implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    private Long totalScore;

    private Integer ratingCount;

    private BigDecimal averageRating;

    @TableField(exist = false)
    private Integer userRating;
}
