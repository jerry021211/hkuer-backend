package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.CreateOrderRequest;

import java.util.Map;

/**
 * 支付服务接口
 */
public interface PayService {

    /**
     * 创建支付订单
     *
     * @param request   创建订单请求
     * @param clientIp  客户端真实 IP
     * @return 支付平台返回的结果（包含 payurl 或 qrcode）
     */
    Result createOrder(CreateOrderRequest request, String clientIp);

    /**
     * 处理支付回调
     *
     * @param params 回调参数（包含所有 POST/GET 参数）
     * @return 处理结果（成功返回 "success"，失败返回错误信息）
     */
    String handleNotify(Map<String, String> params);

    /**
     * 查询订单状态
     *
     * @param orderId 商户订单号
     * @return 订单状态信息
     */
    Result queryOrder(String orderId);
}
