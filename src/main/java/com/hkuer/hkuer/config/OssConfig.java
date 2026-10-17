package com.hkuer.hkuer.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置类
 */
@Data
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.cloud-mode", havingValue = "aliyun", matchIfMissing = true)
public class OssConfig {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id:${aliyun.accessKeyId}}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:${aliyun.accessKeySecret}}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
