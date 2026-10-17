package com.hkuer.hkuer.config;

import com.aliyun.oss.OSS;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.impl.userface.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Explicit opt-in: sends synthetic text and temporary files to the configured cloud services. */
@EnabledIfSystemProperty(named = "hkuer.cloud.live", matches = "true")
class CloudServicesLiveTest {
    private Properties properties() throws Exception {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of(System.getProperty("hkuer.cloud.config")), StandardCharsets.UTF_8)) { p.load(reader); }
        return p;
    }
    @Test void realModeration() throws Exception {
        Properties p = properties();
        ContentSecurityConfig config = new ContentSecurityConfig();
        config.setAccessKeyId(p.getProperty("aliyun.accessKeyId")); config.setAccessKeySecret(p.getProperty("aliyun.accessKeySecret"));
        config.setEndpoint(p.getProperty("aliyun.green.endpoint")); config.setRegionId(p.getProperty("aliyun.green.region-id"));
        assertThat(new ContentSecurityServiceImpl(config.greenClient()).checkText("今天天气很好，祝你学习顺利。" )).isTrue();
        System.out.println("LIVE PASS: moderation API accepted synthetic text");
    }
    @Test @SuppressWarnings("unchecked") void realTranslation() throws Exception {
        Properties p = properties();
        AliyunTranslationServiceImpl service = new AliyunTranslationServiceImpl();
        ReflectionTestUtils.setField(service,"accessKeyId",p.getProperty("aliyun.accessKeyId"));
        ReflectionTestUtils.setField(service,"accessKeySecret",p.getProperty("aliyun.accessKeySecret"));
        ReflectionTestUtils.setField(service,"endpoint",p.getProperty("aliyun.mt.endpoint"));
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String,String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ReflectionTestUtils.setField(service,"redisTemplate",redis);
        service.init();
        Result result = service.translate("今天是美好的一天。", "en");
        assertThat(result.getCode()).isEqualTo("200");
        assertThat(result.getData().toString()).isNotBlank().isNotEqualTo("今天是美好的一天。");
        System.out.println("LIVE PASS: translation API returned translated text");
    }
    @Test void realOssImageAndPdfRoundTrip() throws Exception {
        Properties p = properties();
        OssConfig config = new OssConfig();
        config.setAccessKeyId(p.getProperty("aliyun.oss.access-key-id",p.getProperty("aliyun.accessKeyId")));
        config.setAccessKeySecret(p.getProperty("aliyun.oss.access-key-secret",p.getProperty("aliyun.accessKeySecret")));
        config.setEndpoint(p.getProperty("aliyun.oss.endpoint")); config.setBucketName(p.getProperty("aliyun.oss.bucket-name"));
        OSS client = config.ossClient();
        try {
            OssServiceImpl service = new OssServiceImpl();
            ReflectionTestUtils.setField(service,"ossClient",client); ReflectionTestUtils.setField(service,"ossConfig",config);
            byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
            byte[] pdf = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.US_ASCII);
            for (MockMultipartFile file : new MockMultipartFile[]{new MockMultipartFile("file","cloud-check.png","image/png",png),new MockMultipartFile("file","cloud-check.pdf","application/pdf",pdf)}) {
                String url = service.uploadFile(file,file.getOriginalFilename().endsWith("pdf")?"course-material":"cloud-check");
                String key = URI.create(url).getPath().substring(1);
                try {
                    try (var object = client.getObject(config.getBucketName(),key)) {
                        assertThat(object.getObjectContent().readAllBytes()).isEqualTo(file.getBytes());
                    }
                    HttpResponse<byte[]> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                            .send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
                    assertThat(response.statusCode()).as("Browser-visible OSS URL").isEqualTo(200);
                    assertThat(response.body()).isEqualTo(file.getBytes());
                } finally { service.deleteFile(url); }
                assertThat(client.doesObjectExist(config.getBucketName(),key)).isFalse();
            }
            System.out.println("LIVE PASS: OSS image/PDF upload, public read and cleanup");
        } finally { client.shutdown(); }
    }
}
