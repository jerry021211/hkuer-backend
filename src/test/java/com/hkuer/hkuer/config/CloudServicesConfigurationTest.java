package com.hkuer.hkuer.config;

import com.hkuer.hkuer.service.impl.userface.*;
import com.hkuer.hkuer.service.userface.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class CloudServicesConfigurationTest {
    static ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withInitializer(c -> c.getEnvironment().setActiveProfiles("mesh"))
                .withUserConfiguration(MeshDevConfig.class, ContentSecurityConfig.class, OssConfig.class,
                        ContentSecurityServiceImpl.class, AliyunTranslationServiceImpl.class,
                        OssServiceImpl.class, LocalStorageService.class, MailModeConfigurationTest.RedisStub.class)
                .withPropertyValues("mesh.dev.root=target/test-cloud", "mesh.redis-prefix=test:", "mesh.mail-mode=local");
    }
    @Test void realModeHasNoLocalReplacement() {
        runner().withPropertyValues("mesh.cloud-mode=aliyun", "aliyun.accessKeyId=test", "aliyun.accessKeySecret=test",
                "aliyun.green.endpoint=green-cip.cn-shanghai.aliyuncs.com", "aliyun.green.region-id=cn-shanghai",
                "aliyun.mt.endpoint=mt.cn-hangzhou.aliyuncs.com", "aliyun.oss.endpoint=oss-cn-shenzhen.aliyuncs.com",
                "aliyun.oss.bucket-name=test-bucket").run(c -> {
            assertThat(c).hasNotFailed().hasSingleBean(ContentSecurityService.class)
                    .hasSingleBean(AliyunTranslationService.class).hasSingleBean(OssService.class);
            assertThat(c.getBean(ContentSecurityService.class)).isInstanceOf(ContentSecurityServiceImpl.class);
            assertThat(c.getBean(AliyunTranslationService.class)).isInstanceOf(AliyunTranslationServiceImpl.class);
            assertThat(c.getBean(OssService.class)).isInstanceOf(OssServiceImpl.class);
            assertThat(c).doesNotHaveBean(LocalStorageService.class).doesNotHaveBean("localContentSecurity").doesNotHaveBean("localTranslation");
        });
    }
    @Test void explicitlyLocalTestsNeedNoCloudCredentials() {
        runner().withPropertyValues("mesh.cloud-mode=local").run(c -> {
            assertThat(c).hasNotFailed().hasSingleBean(ContentSecurityService.class)
                    .hasSingleBean(AliyunTranslationService.class).hasSingleBean(OssService.class);
            assertThat(c.getBean(OssService.class)).isInstanceOf(LocalStorageService.class);
        });
    }
    @Test void ossUsesItsOwnConfiguredCredentials() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withUserConfiguration(OssConfig.class)
                .withPropertyValues("aliyun.accessKeyId=general", "aliyun.accessKeySecret=general-secret",
                        "aliyun.oss.access-key-id=oss-specific", "aliyun.oss.access-key-secret=oss-secret",
                        "aliyun.oss.endpoint=oss-cn-shenzhen.aliyuncs.com", "aliyun.oss.bucket-name=test-bucket")
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    assertThat(c.getBean(OssConfig.class).getAccessKeyId()).isEqualTo("oss-specific");
                    assertThat(c.getBean(OssConfig.class).getAccessKeySecret()).isEqualTo("oss-secret");
                });
    }
}
