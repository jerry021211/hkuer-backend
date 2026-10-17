package com.hkuer.hkuer.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体
 */
@TableName("pay_order")
@Data
public class PayOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户订单号 */
    private String orderId;

    /** 支付平台订单号 */
    private String tradeNo;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 实际支付金额（元，回调时更新） */
    private BigDecimal actualAmount;

    /** 支付方式 */
    private String payType;

    /** 下单用户 ID，只能由服务端从登录态写入 */
    private String userId;

    /** 业务类型：AD_POST_PUBLISH-广告帖发布, POST_TOP-帖子置顶 */
    private String businessType;

    /** 业务对象 ID，目前对应帖子 ID */
    private Long businessId;

    /** 订单状态：0-待支付, 1-已支付, 2-已关闭, 3-已退款, 4-支付金额异常 */
    private Integer status;

    /** 商品名称 */
    private String goodsName;

    /** 商品描述 */
    private String goodsDetail;

    /** 客户端 IP */
    private String clientIp;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
