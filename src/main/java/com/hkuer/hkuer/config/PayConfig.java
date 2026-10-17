package com.hkuer.hkuer.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 易支付/码支付配置类
 */
@Data
@Configuration
public class PayConfig {

    /** 支付平台 API 地址（mapi.php） */
    @Value("${pay.mzf.api-url}")
    private String apiUrl;

    /** 商户 ID（PID） */
    @Value("${pay.mzf.pid}")
    private String pid;

    /** 商户密钥（KEY） */
    @Value("${pay.mzf.key}")
    private String key;

    /** 异步通知地址（支付平台回调此地址） */
    @Value("${pay.mzf.notify-url}")
    private String notifyUrl;

    /** 同步跳转地址（支付完成后跳转） */
    @Value("${pay.mzf.return-url}")
    private String returnUrl;
}
