package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

import java.io.Serializable;
/**
 * 创建支付订单请求 DTO
 */
@Data
public class CreateOrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 支付方式：alipay-支付宝, wxpay-微信支付, qqpay-QQ钱包等 */
    private String payType;

    /** 业务类型：AD_POST_PUBLISH-广告帖发布, POST_TOP-帖子置顶 */
    private String businessType;

    /** 业务对象 ID，目前对应帖子 ID */
    private Long businessId;
}
