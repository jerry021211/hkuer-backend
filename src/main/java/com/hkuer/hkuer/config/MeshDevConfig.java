package com.hkuer.hkuer.config;

import com.hkuer.hkuer.service.userface.*;
import com.hkuer.hkuer.common.Result;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Local providers for the default development environment; not production integrations. */
@Configuration
@Profile("mesh")
@lombok.extern.slf4j.Slf4j
public class MeshDevConfig implements WebMvcConfigurer {
    @Value("${mesh.dev.root}") private String root;
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.mail-mode", havingValue = "local")
    public AliyunMailService localMail() {
        return (to, subject, content) -> {
            try {
                Path directory=Path.of(root,"mail"); Files.createDirectories(directory);
                Path file = directory.resolve(UUID.randomUUID()+".html");
                Files.writeString(file, content, StandardCharsets.UTF_8);
                log.info("测试邮件已保存到本地文件，未发送到外部邮箱: {}", file);
                return true;
            } catch (java.io.IOException e) { throw new IllegalStateException("无法写入本地邮件箱",e); }
        };
    }
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.cloud-mode", havingValue = "local")
    public ContentSecurityService localContentSecurity() { return content -> true; }
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.cloud-mode", havingValue = "local")
    public AliyunTranslationService localTranslation() {
        return (text,lang) -> Result.error("503","独立开发环境未配置翻译服务");
    }
    @Bean @Primary public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory,
            @Value("${mesh.redis-prefix}") String prefix) {
        StringRedisTemplate template=new StringRedisTemplate(factory);
        template.setKeySerializer(new StringRedisSerializer() {
            @Override public byte[] serialize(String key) { return key==null?null:super.serialize(prefix+key); }
            @Override public String deserialize(byte[] bytes) {
                String key=super.deserialize(bytes);
                return key!=null && key.startsWith(prefix)?key.substring(prefix.length()):key;
            }
        });
        return template;
    }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**").addResourceLocations(Path.of(root,"uploads").toUri().toString());
    }
}
