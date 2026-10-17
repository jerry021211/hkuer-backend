package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.CreateOrderRequest;
import com.hkuer.hkuer.service.userface.PayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /**
     * 统一下单接口
     * 创建支付订单并返回支付信息（二维码或跳转链接）
     *
     * @param request     创建订单请求
     * @param httpRequest HTTP 请求对象（用于获取客户端 IP）
     * @return 支付信息（包含 qrcode 或 payurl）
     */
    @PostMapping("/createOrder")
    public Result createOrder(@RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        // 获取客户端真实 IP
        String clientIp = getClientRealIp(httpRequest);
        log.info("创建支付订单，业务类型: {}, 业务ID: {}, 支付方式: {}, 客户端IP: {}",
                request.getBusinessType(), request.getBusinessId(), request.getPayType(), clientIp);

        return payService.createOrder(request, clientIp);
    }

    /**
     * 支付回调接口
     * 接收支付平台的异步通知
     */
    @RequestMapping(value = "/notify", method = {RequestMethod.POST, RequestMethod.GET})
    public String notify(HttpServletRequest request) {
        // 🔥🔥🔥 1. 进门第一件事：先吼一声！看看是不是进来了！
        log.error("🔥🔥🔥 [回调入口] 收到回调请求！Method: {}, IP: {}", request.getMethod(), request.getRemoteAddr());

        // 获取所有请求参数
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        // 🔥🔥🔥 2. 打印看看收到了什么参数（如果有参数，说明数据传输正常）
        log.error("🔥🔥🔥 [回调参数] 原始内容: {}", params);

        if (params.isEmpty()) {
            log.warn("⚠️ [回调警告] 参数为空！可能是 Content-Type 不对，或者 Body 是 JSON 格式？");
            // 如果是 JSON 格式，request.getParameterMap() 是拿不到数据的，但这不影响我们先测试通不通
        }

        // 处理回调业务
        try {
            String result = payService.handleNotify(params);
            log.info("✅ [回调结束] 业务处理结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("❌ [回调异常] 处理逻辑报错", e);
            return "fail";
        }
    }

    /**
     * 查询订单状态
     *
     * @param orderId 商户订单号
     * @return 订单状态信息
     */
    @GetMapping("/queryOrder")
    public Result queryOrder(@RequestParam String orderId) {
        return payService.queryOrder(orderId);
    }

    /**
     * 获取客户端真实 IP
     * 考虑代理和负载均衡的情况
     *
     * @param request HTTP 请求
     * @return 客户端真实 IP
     */
    private String getClientRealIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
