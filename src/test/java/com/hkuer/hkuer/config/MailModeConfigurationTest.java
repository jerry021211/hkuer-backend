package com.hkuer.hkuer.config;

import com.hkuer.hkuer.service.userface.AliyunMailService;
import com.hkuer.hkuer.service.impl.userface.AliyunMailServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MailModeConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("mesh"))
            .withUserConfiguration(MeshDevConfig.class, AliyunMailServiceImpl.class, RedisStub.class)
            .withPropertyValues("mesh.dev.root=target/test-mail", "mesh.redis-prefix=test:");

    @Test
    void aliyunModeSelectsRealSenderWithoutSendingMail() {
        runner.withPropertyValues("mesh.mail-mode=aliyun",
                "aliyun.mail.accessKeyId=test", "aliyun.mail.accessKeySecret=test",
                "aliyun.mail.accountName=test@example.test", "aliyun.mail.regionId=cn-hangzhou")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AliyunMailService.class);
                    assertThat(context.getBean(AliyunMailService.class)).isInstanceOf(AliyunMailServiceImpl.class);
                });
    }

    @Test
    void localModeNeedsNoCloudCredentialsAndSelectsOnlyLocalSender() {
        runner.withPropertyValues("mesh.mail-mode=local").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AliyunMailService.class);
            assertThat(context).doesNotHaveBean(AliyunMailServiceImpl.class);
            assertThat(context).hasBean("localMail");
        });
    }

    @Test
    void missingRealCredentialsFailsInsteadOfSilentlyUsingLocalMail() {
        runner.withPropertyValues("mesh.mail-mode=aliyun").run(context -> {
            assertThat(context).hasFailed();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class RedisStub {
        @Bean RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }
    }
}
