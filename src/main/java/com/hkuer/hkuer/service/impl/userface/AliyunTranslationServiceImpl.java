package com.hkuer.hkuer.service.impl.userface;

import com.aliyun.alimt20181012.Client;
import com.aliyun.alimt20181012.models.TranslateGeneralRequest;
import com.aliyun.alimt20181012.models.TranslateGeneralResponse;
import com.aliyun.teaopenapi.models.Config;
import com.hkuer.hkuer.common.Result; // 【引入 Result 类】
import com.hkuer.hkuer.service.userface.AliyunTranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.cloud-mode", havingValue = "aliyun", matchIfMissing = true)
public class AliyunTranslationServiceImpl implements AliyunTranslationService {

    @Value("${aliyun.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.mt.endpoint}")
    private String endpoint;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Client client;

    @PostConstruct
    public void init() {
        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = endpoint;
            this.client = new Client(config);
            log.info("阿里云翻译服务初始化成功");
        } catch (Exception e) {
            throw new IllegalStateException("阿里云翻译服务初始化失败", e);
        }
    }
    private boolean containsChinese(String text) {
        // \u4e00-\u9fa5 是常用汉字的 Unicode 范围
        return Pattern.compile("[\\u4e00-\\u9fa5]").matcher(text).find();
    }
    /**
     * 翻译核心方法
     * 修改返回值类型为 Result
     */
    @Override
    public Result translate(String sourceText, String targetLang) {
        // 1. 参数校验
        if (sourceText == null || sourceText.trim().isEmpty()) {
            return Result.error("400", "翻译内容不能为空");
        }
        if (targetLang == null || targetLang.isEmpty() || "auto".equals(targetLang)) {
            if (containsChinese(sourceText)) {
                // 原文有中文 -> 译成英文
                targetLang = "en";
            } else {
                // 原文没中文 -> 译成中文
                targetLang = "zh";
            }
        }

        // 2. 生成 Redis 缓存 Key
        String textHash = DigestUtils.md5DigestAsHex(sourceText.getBytes(StandardCharsets.UTF_8));
        String cacheKey = "trans:" + targetLang + ":" + textHash;

        // 3. 查询 Redis 缓存
        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null && !cachedResult.isEmpty()) {
            // log.info("命中翻译缓存");
            // 【修改点】命中缓存，返回成功 Result
            return Result.success(cachedResult);
        }

        // 4. 缓存未命中，调用阿里云 API
        try {
            TranslateGeneralRequest request = new TranslateGeneralRequest()
                    .setFormatType("text")
                    .setSourceLanguage("auto")
                    .setTargetLanguage(targetLang)
                    .setSourceText(sourceText)
                    .setScene("general");

            TranslateGeneralResponse response = client.translateGeneral(request);
            if (response == null || response.getBody() == null ||
                    !Integer.valueOf(200).equals(response.getBody().getCode()) || response.getBody().getData() == null) {
                return Result.error("503", "翻译服务暂时不可用");
            }
            String translatedText = response.getBody().getData().getTranslated();
            if (translatedText == null || translatedText.isBlank()) return Result.error("503", "翻译服务未返回结果");

            // 5. 存入 Redis
            if (translatedText != null && !translatedText.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, translatedText, 7, TimeUnit.DAYS);
            }

            // 【修改点】调用成功，返回成功 Result
            return Result.success(translatedText);

        } catch (Exception e) {
            log.error("翻译 API 调用失败: text={}, target={}", sourceText, targetLang, e);

            // 【修改点】发生异常，返回失败 Result
            // 这里你可以选择返回 500 错误，让前端提示“翻译失败”
            return Result.error("500", "翻译服务暂时不可用");

            // 或者，如果你想降级处理（不报错，直接显示原文），可以使用：
            // return Result.success(sourceText);
        }
    }
}
