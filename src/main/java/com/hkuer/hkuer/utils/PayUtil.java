package com.hkuer.hkuer.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 易支付/码支付签名工具类
 * 严格按照官方 PHP SDK 逻辑重构
 */
@Slf4j
public class PayUtil {

    /**
     * 生成签名
     */
    public static String generateSign(Map<String, Object> params, String key) {
        // 1. 剔除空值、sign 和 sign_type 参数
        Map<String, String> filteredParams = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramKey = entry.getKey();
            Object valueObj = entry.getValue();

            if (valueObj != null) {
                String paramValue = String.valueOf(valueObj);
                // 必须同时跳过 sign 和 sign_type。
                // 使用 isNotEmpty 对齐 PHP 的 $value !== ''
                if (!"sign".equals(paramKey) && !"sign_type".equals(paramKey) && StrUtil.isNotEmpty(paramValue)) {
                    filteredParams.put(paramKey, paramValue);
                }
            }
        }

        // 2. 按字典序升序排序（TreeMap 已自动排序）
        // 3. 拼接成 key=value&key=value 格式
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filteredParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        // 4. 最后直接拼接商户密钥
        sb.append(key);

        String signStr = sb.toString();

        // 5. MD5 加密（强制 UTF-8 编码防中文乱码，且强制转小写！）
        String sign = DigestUtil.md5Hex(signStr.getBytes(StandardCharsets.UTF_8)).toLowerCase();

        log.info("签名原始明文字符串: {}", signStr);
        log.info("生成的小写MD5签名: {}", sign);

        return sign;
    }

    /**
     * 验证签名
     */
    public static boolean verifySign(Map<String, Object> params, String key) {
        Object receivedSignObj = params.get("sign");
        if (receivedSignObj == null || StrUtil.isBlank(String.valueOf(receivedSignObj))) {
            log.warn("回调参数中缺少 sign 字段");
            return false;
        }
        String receivedSign = String.valueOf(receivedSignObj);

        String calculatedSign = generateSign(params, key);

        // 比对签名（忽略大小写，兼容性更好）
        boolean isValid = receivedSign.equalsIgnoreCase(calculatedSign);

        if (!isValid) {
            log.warn("签名验证失败！接收到的签名: {}, 计算出的签名: {}", receivedSign, calculatedSign);
        } else {
            log.debug("签名验证成功");
        }

        return isValid;
    }

    /**
     * 将请求参数 Map 转换为 URL 查询字符串
     */
    public static String buildQueryString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object valueObj = entry.getValue();
            if (valueObj != null) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(entry.getKey()).append("=").append(String.valueOf(valueObj));
            }
        }
        return sb.toString();
    }
}