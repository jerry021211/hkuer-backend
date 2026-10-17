package com.hkuer.hkuer.config;

// 🟢 修改点：使用了新版 SDK 的包路径
import com.aliyun.green20220302.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云内容安全（Green 2022 标准版）配置类
 */
@Data
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.cloud-mode", havingValue = "aliyun", matchIfMissing = true)
public class ContentSecurityConfig {

    @Value("${aliyun.green.endpoint}")
    private String endpoint;

    @Value("${aliyun.green.region-id}")
    private String regionId;

    @Value("${aliyun.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.accessKeySecret}")
    private String accessKeySecret;

    @Bean
    public Client greenClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint)
                .setRegionId(regionId);

        // 建议：设置超时时间，避免网络波动导致接口卡死
        config.setReadTimeout(5000);
        config.setConnectTimeout(3000);

        return new Client(config);
    }
}