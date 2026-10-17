package com.hkuer.hkuer.service.impl.userface;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationRequest;
import com.aliyun.green20220302.models.TextModerationResponse;
import com.aliyun.green20220302.models.TextModerationResponseBody;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkuer.hkuer.exception.ContentRiskException;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.service.userface.ContentSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 内容安全服务实现
 * 使用阿里云 Green 2022 进行文本内容安全检测
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.cloud-mode", havingValue = "aliyun", matchIfMissing = true)
public class ContentSecurityServiceImpl implements ContentSecurityService {

    private final Client greenClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean checkText(String content) {
        if (content == null || content.trim().isEmpty()) {
            return true; // 空内容直接放行
        }

        try {
            // 1. 构建请求
            TextModerationRequest request = new TextModerationRequest();
            // 确保使用 comment_detection (标准版) 或 ugc_moderation_byllm_pro (UGC大模型)
            request.setService("comment_detection");
            request.setServiceParameters(buildServiceParameters(content));

            // 2. 调用接口 (生产环境不建议打印大量 Info 日志)
            RuntimeOptions runtime = new RuntimeOptions();
            TextModerationResponse response = greenClient.textModerationWithOptions(request, runtime);

            // 3. 基础响应校验
            if (response == null || response.getBody() == null) {
                throw new BusinessException("503", "内容审核服务暂时不可用，请稍后重试");
            }

            TextModerationResponseBody body = response.getBody();
            // 检查业务状态码 (200 为成功)
            if (body.getCode() == null || body.getCode() != 200) {
                log.warn("内容安全检测接口失败: code={}, msg={}", body.getCode(), body.getMessage());
                throw new BusinessException("503", "内容审核服务暂时不可用，请稍后重试");
            }

            if (body.getData() == null) {
                throw new BusinessException("503", "内容审核服务未返回结果，请稍后重试");
            }

            // 4. 解析核心结果 (关键修复点)
            // 将 Data 对象转换为 JsonNode，以便灵活处理 labels 和 reason
            String dataStr = objectMapper.writeValueAsString(body.getData());
            JsonNode dataNode = objectMapper.readTree(dataStr);

            // 🟢 修复逻辑：labels 字段是字符串 (例如 "profanity,political") 而非数组
            if (dataNode.has("labels")) {
                String labels = dataNode.get("labels").asText();

                // 只要 labels 不为空字符串，就代表命中了风险
                if (labels != null && !labels.trim().isEmpty()) {

                    // 进一步解析 reason 字段 (它是一个嵌套的 JSON 字符串)
                    String detailedReason = parseReasonTips(dataNode);

                    // 只在拦截时打印关键日志
                    log.warn("🚫 内容违规拦截 | 标签: [{}] | 原因: [{}] | 内容摘要: {}",
                            labels,
                            detailedReason,
                            content.length() > 20 ? content.substring(0, 20) + "..." : content);

                    // 抛出异常阻断发布
                    throw new ContentRiskException("400", "内容包含违规信息(" + detailedReason + ")，请修改");
                }
            }

            // 5. 安全，正常返回
            return true;

        } catch (ContentRiskException e) {
            throw e; // 业务异常直接抛出
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("内容安全检测系统异常，本次未通过审核。错误: {}", e.getMessage());
            throw new BusinessException("503", "内容审核服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 构建参数 JSON
     */
    private String buildServiceParameters(String content) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("content", content);
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            // 兜底简单的 JSON 构建
            return "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}";
        }
    }

    /**
     * 辅助方法：解析 reason 字段中的 riskTips (风险提示)
     * reason 示例: "{\"riskLevel\":\"medium\",\"riskTips\":\"辱骂_低俗\",\"riskWords\":\"xx\"}"
     */
    private String parseReasonTips(JsonNode dataNode) {
        String defaultTips = "违规内容";
        try {
            if (dataNode.has("reason")) {
                String reasonJsonStr = dataNode.get("reason").asText();
                if (reasonJsonStr != null && !reasonJsonStr.isEmpty()) {
                    JsonNode reasonNode = objectMapper.readTree(reasonJsonStr);
                    if (reasonNode.has("riskTips")) {
                        return reasonNode.get("riskTips").asText();
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败不影响主流程
        }
        return defaultTips;
    }
}
