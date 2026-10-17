package com.hkuer.hkuer.service.impl.userface;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.config.PayConfig;
import com.hkuer.hkuer.entity.DTO.CreateOrderRequest;
import com.hkuer.hkuer.entity.PO.PayOrder;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.mapper.userface.PayOrderMapper;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.PayService;
import com.hkuer.hkuer.utils.PayUtil;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 支付服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private static final String BUSINESS_AD_POST_PUBLISH = "AD_POST_PUBLISH";
    private static final String BUSINESS_POST_TOP = "POST_TOP";
    private static final BigDecimal AD_POST_PRICE = new BigDecimal("0.02");
    private static final BigDecimal POST_TOP_PRICE = new BigDecimal("0.02");
    private static final int STATUS_AMOUNT_MISMATCH = 4;
    private static final Set<String> SUPPORTED_PAY_TYPES = Set.of("alipay", "wxpay", "qqpay");

    private final PayConfig payConfig;
    private final PayOrderMapper payOrderMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Result createOrder(CreateOrderRequest request, String clientIp) {
        try {
            // 1. 身份与参数校验。订单号、金额和商品信息都由服务端生成，不能信任前端。
            String userId = UserContext.getCurrentUserId();
            if (StrUtil.isBlank(userId)) {
                return Result.error("401", "请先登录");
            }
            if (request == null || StrUtil.isBlank(request.getPayType())) {
                return Result.error("400", "支付方式不能为空");
            }
            if (!SUPPORTED_PAY_TYPES.contains(request.getPayType())) {
                return Result.error("400", "不支持的支付方式");
            }
            if (StrUtil.isBlank(request.getBusinessType()) || request.getBusinessId() == null) {
                return Result.error("400", "支付业务信息不能为空");
            }

            // 2. 校验业务对象，同时由服务端确定价格与商品信息。
            // 最终状态会在回调事务中再次加锁校验，此处不在外部 HTTP 请求期间持有帖子行锁。
            Post post = postMapper.selectById(request.getBusinessId());
            if (post == null) {
                return Result.error("404", "帖子不存在");
            }

            BigDecimal amount;
            String goodsName;
            String goodsDetail;
            if (BUSINESS_AD_POST_PUBLISH.equals(request.getBusinessType())) {
                if (!userId.equals(post.getAuthorId())) {
                    return Result.error("403", "只能为自己的帖子付款");
                }
                if (post.getStatus() == null || post.getStatus() != 3) {
                    return Result.error("400", "该帖子当前不需要支付发布费用");
                }
                amount = AD_POST_PRICE;
                goodsName = "Ad Post Promotion";
                goodsDetail = "Publish blocked post";
            } else if (BUSINESS_POST_TOP.equals(request.getBusinessType())) {
                User user = userMapper.selectById(userId);
                if (user == null || user.getUsertype() == null || user.getUsertype() != 1) {
                    return Result.error("403", "只有管理员可以执行帖子置顶");
                }
                if (post.getStatus() == null || post.getStatus() != 1) {
                    return Result.error("400", "只能置顶已发布的帖子");
                }
                if (post.getIsTop() != null && post.getIsTop() == 1) {
                    return Result.error("400", "帖子已经置顶");
                }
                amount = POST_TOP_PRICE;
                goodsName = "Pin Post";
                goodsDetail = "Pin post to homepage";
            } else {
                return Result.error("400", "不支持的支付业务类型");
            }

            // 3. 保存服务端生成的支付订单。
            String orderId = "PAY_" + UUID.randomUUID().toString().replace("-", "");
            PayOrder order = new PayOrder();
            order.setOrderId(orderId);
            order.setAmount(amount);
            order.setPayType(request.getPayType());
            order.setUserId(userId);
            order.setBusinessType(request.getBusinessType());
            order.setBusinessId(request.getBusinessId());
            order.setGoodsName(goodsName);
            order.setGoodsDetail(goodsDetail);
            order.setClientIp(clientIp);
            order.setStatus(0); // 待支付
            order.setCreateTime(LocalDateTime.now().withNano(0));
            order.setUpdateTime(LocalDateTime.now().withNano(0));
            if (payOrderMapper.insert(order) != 1) {
                throw new IllegalStateException("保存支付订单失败");
            }

            // 4. 组装支付平台请求参数
            Map<String, Object> payParams = new HashMap<>();
            payParams.put("pid", payConfig.getPid());
            payParams.put("type", request.getPayType()); // alipay, wxpay 等
            payParams.put("out_trade_no", orderId); // 商户订单号
            payParams.put("notify_url", payConfig.getNotifyUrl()); // 异步通知地址
            payParams.put("return_url", payConfig.getReturnUrl()); // 同步跳转地址
            payParams.put("name", goodsName);
            payParams.put("money", amount.setScale(2, RoundingMode.HALF_UP).toString());
            payParams.put("clientip", clientIp); // 客户端 IP

            // 5. 生成签名
            String sign = PayUtil.generateSign(payParams, payConfig.getKey());
            payParams.put("sign", sign);
            payParams.put("sign_type", "MD5"); // 签名类型

            // 6. 调用支付平台接口
            log.info("========== 发起支付请求 ==========");
            log.info("订单号: {}", orderId);
            log.info("金额: {}", amount);
            log.info("支付方式: {}", request.getPayType());
            log.info("请求参数: {}", payParams);
            log.info("================================");

            String responseBody = HttpUtil.post(payConfig.getApiUrl(), payParams);
            log.info("支付平台响应: {}", responseBody);

            // 7. 解析响应
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            // 检查响应状态码
            int code = responseJson.has("code") ? responseJson.get("code").asInt() : -1;
            if (code != 1) {
                String msg = responseJson.has("msg") ? responseJson.get("msg").asText() : "支付平台返回错误";
                log.error("支付平台返回错误，code: {}, msg: {}", code, msg);
                order.setStatus(2);
                order.setUpdateTime(LocalDateTime.now().withNano(0));
                payOrderMapper.updateById(order);
                return Result.error("500", "创建支付订单失败: " + msg);
            }

            if (responseJson.has("money")) {
                BigDecimal platformAmount = new BigDecimal(responseJson.get("money").asText());
                if (platformAmount.compareTo(amount) != 0) {
                    order.setStatus(2);
                    order.setUpdateTime(LocalDateTime.now().withNano(0));
                    payOrderMapper.updateById(order);
                    log.error("支付平台下单金额不一致，订单号: {}, 应付: {}, 平台返回: {}",
                            orderId, amount, platformAmount);
                    return Result.error("500", "支付平台返回金额异常");
                }
            }

            if (responseJson.has("trade_no")) {
                order.setTradeNo(responseJson.get("trade_no").asText());
                order.setUpdateTime(LocalDateTime.now().withNano(0));
                payOrderMapper.updateById(order);
            }

            // 8. 返回支付信息给前端
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", orderId);
            result.put("payType", request.getPayType());
            result.put("amount", amount);
            result.put("businessType", request.getBusinessType());
            result.put("businessId", request.getBusinessId());
            
            // 如果有二维码，返回 qrcode
            if (responseJson.has("qrcode")) {
                result.put("qrcode", responseJson.get("qrcode").asText());
            }
            
            // 如果有支付链接，返回 payurl
            if (responseJson.has("payurl")) {
                result.put("payurl", responseJson.get("payurl").asText());
            }
            
            // 如果有跳转链接，返回 url
            if (responseJson.has("url")) {
                result.put("url", responseJson.get("url").asText());
            }

            if (responseJson.has("urlscheme")) {
                result.put("urlscheme", responseJson.get("urlscheme").asText());
            }

            return Result.success(result);

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("创建支付订单异常", e);
            return Result.error("500", "创建支付订单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(Map<String, String> params) {
        try {
            log.info("========== 收到支付回调 ==========");
            log.info("回调参数: {}", params);
            log.info("=================================");

            // 1. 回调只能依赖平台签名鉴权，同时核对商户身份。
            boolean signValid = PayUtil.verifySign(new HashMap<String, Object>(params), payConfig.getKey());
            if (!signValid) {
                log.error("支付回调签名验证失败！订单号: {}", params.get("out_trade_no"));
                return "fail";
            }
            if (!payConfig.getPid().equals(params.get("pid"))) {
                log.error("支付回调商户ID不匹配！订单号: {}", params.get("out_trade_no"));
                return "fail";
            }

            // 2. 获取订单信息
            String orderId = params.get("out_trade_no");
            String tradeNo = params.get("trade_no"); // 支付平台订单号
            String money = params.get("money"); // 实际支付金额
            String tradeStatus = params.get("trade_status"); // 交易状态

            if (StrUtil.isBlank(orderId) || StrUtil.isBlank(tradeNo) || StrUtil.isBlank(money)) {
                log.error("回调参数不完整，订单号: {}", orderId);
                return "fail";
            }

            // 未支付成功的通知无需进入业务处理。
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"success".equals(tradeStatus)) {
                log.warn("交易状态异常/未完成: {}, 订单号: {}", tradeStatus, orderId);
                return "success";
            }

            // 3. 锁定订单，保证重复或并发回调只履行业务一次。
            PayOrder order = payOrderMapper.selectOne(
                    new LambdaQueryWrapper<PayOrder>()
                            .eq(PayOrder::getOrderId, orderId)
                            .last("FOR UPDATE")
            );

            if (order == null) {
                log.error("订单不存在: {}", orderId);
                return "fail";
            }

            // 4. 检查订单状态（防止重复处理）
            if (order.getStatus() == 1 || order.getStatus() == STATUS_AMOUNT_MISMATCH) {
                log.warn("订单 {} 已处理过，忽略重复回调", orderId);
                return "success";
            }
            if (order.getStatus() == null || order.getStatus() != 0) {
                log.error("订单状态不允许支付，订单号: {}, 状态: {}", orderId, order.getStatus());
                return "fail";
            }
            if (StrUtil.isBlank(order.getUserId()) || StrUtil.isBlank(order.getBusinessType())
                    || order.getBusinessId() == null) {
                log.error("支付订单缺少业务归属信息，订单号: {}", orderId);
                return "fail";
            }
            if (StrUtil.isNotBlank(params.get("type")) && !order.getPayType().equals(params.get("type"))) {
                log.error("支付方式不匹配，订单号: {}, 应付方式: {}, 回调方式: {}",
                        orderId, order.getPayType(), params.get("type"));
                return "fail";
            }
            if (StrUtil.isNotBlank(order.getTradeNo()) && !order.getTradeNo().equals(tradeNo)) {
                log.error("平台订单号不匹配，商户订单号: {}", orderId);
                return "fail";
            }

            // 5. 金额必须与服务端创建订单时保存的应付金额完全一致。
            BigDecimal actualAmount = new BigDecimal(money);
            BigDecimal orderAmount = order.getAmount();
            if (orderAmount == null || actualAmount.compareTo(orderAmount) != 0) {
                log.error("支付金额不匹配！订单金额: {}, 实际支付: {}, 订单号: {}",
                        orderAmount, actualAmount, orderId);
                // 平台确实已经收款，因此记录异常并确认收到回调，但绝不履行业务。
                order.setStatus(STATUS_AMOUNT_MISMATCH);
                order.setTradeNo(tradeNo);
                order.setActualAmount(actualAmount);
                order.setPayTime(LocalDateTime.now().withNano(0));
                order.setUpdateTime(LocalDateTime.now().withNano(0));
                if (payOrderMapper.updateById(order) != 1) {
                    throw new IllegalStateException("记录支付金额异常失败");
                }
                return "success";
            }

            // 6. 先履行业务，再更新支付订单；二者处于同一事务，任一步失败都会回滚。
            fulfillBusiness(order);

            order.setStatus(1); // 已支付
            order.setTradeNo(tradeNo);
            order.setActualAmount(actualAmount);
            order.setPayTime(LocalDateTime.now().withNano(0));
            order.setUpdateTime(LocalDateTime.now().withNano(0));
            if (payOrderMapper.updateById(order) != 1) {
                throw new IllegalStateException("更新支付订单状态失败");
            }

            log.info("订单支付及业务履行成功！订单号: {}, 支付平台订单号: {}, 金额: {}, 业务: {}:{}",
                    orderId, tradeNo, actualAmount, order.getBusinessType(), order.getBusinessId());

            return "success";

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("处理支付回调异常", e);
            return "fail";
        }
    }

    private void fulfillBusiness(PayOrder order) {
        Post post = postMapper.selectByIdForUpdate(order.getBusinessId());
        if (post == null) {
            throw new IllegalStateException("支付关联的帖子不存在");
        }

        if (BUSINESS_AD_POST_PUBLISH.equals(order.getBusinessType())) {
            if (!order.getUserId().equals(post.getAuthorId())) {
                throw new IllegalStateException("支付订单用户与帖子作者不匹配");
            }
            if (post.getStatus() != null && post.getStatus() == 1) {
                return; // 另一笔有效订单已完成发布，当前付款仍需如实入账。
            }
            if (post.getStatus() == null || post.getStatus() != 3) {
                throw new IllegalStateException("帖子状态不允许支付发布");
            }

            Post updatePost = new Post();
            updatePost.setId(post.getId());
            updatePost.setStatus(1);
            if (postMapper.updateById(updatePost) != 1) {
                throw new IllegalStateException("发布付费帖子失败");
            }
            return;
        }

        if (BUSINESS_POST_TOP.equals(order.getBusinessType())) {
            if (post.getStatus() == null || post.getStatus() != 1) {
                throw new IllegalStateException("帖子状态不允许置顶");
            }
            if (post.getIsTop() != null && post.getIsTop() == 1) {
                return;
            }

            Post updatePost = new Post();
            updatePost.setId(post.getId());
            updatePost.setIsTop(1);
            updatePost.setTopTime(LocalDateTime.now().withNano(0));
            if (postMapper.updateById(updatePost) != 1) {
                throw new IllegalStateException("付费置顶帖子失败");
            }
            return;
        }

        throw new IllegalStateException("未知的支付业务类型");
    }

    @Override
    public Result queryOrder(String orderId) {
        try {
            String userId = UserContext.getCurrentUserId();
            if (StrUtil.isBlank(userId)) {
                return Result.error("401", "请先登录");
            }
            if (StrUtil.isBlank(orderId)) {
                return Result.error("400", "订单号不能为空");
            }

            PayOrder order = payOrderMapper.selectOne(
                    new LambdaQueryWrapper<PayOrder>()
                            .eq(PayOrder::getOrderId, orderId)
                            .eq(PayOrder::getUserId, userId));
            
            if (order == null) {
                return Result.error("404", "订单不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getOrderId());
            result.put("tradeNo", order.getTradeNo());
            result.put("amount", order.getAmount());
            result.put("actualAmount", order.getActualAmount());
            result.put("payType", order.getPayType());
            result.put("businessType", order.getBusinessType());
            result.put("businessId", order.getBusinessId());
            result.put("status", order.getStatus());
            result.put("fulfilled", order.getStatus() != null && order.getStatus() == 1);
            result.put("statusText", getStatusText(order.getStatus()));
            result.put("createTime", order.getCreateTime());
            result.put("payTime", order.getPayTime());

            return Result.success(result);
        } catch (Exception e) {
            log.error("查询订单异常", e);
            return Result.error("500", "查询订单失败");
        }
    }

    /**
     * 获取订单状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已关闭";
            case 3: return "已退款";
            case STATUS_AMOUNT_MISMATCH: return "支付金额异常";
            default: return "未知";
        }
    }
}
