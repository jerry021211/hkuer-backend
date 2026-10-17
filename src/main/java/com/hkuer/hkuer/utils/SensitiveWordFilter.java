package com.hkuer.hkuer.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 敏感词过滤器 - 基于DFA算法
 * 支持敏感词检测、替换、统计
 */
@Component
@Slf4j
public class SensitiveWordFilter {

    private Map<String, Object> sensitiveWordMap;
    private static final String END_FLAG = "isEnd";
    private static final String REPLACEMENT = "***"; // 替换字符
    private static final String SENSITIVE_WORD_FILE = "sensitive_words.txt"; // 敏感词库文件路径

    /**
     * 初始化敏感词库
     */
    @PostConstruct
    public void init() {
        sensitiveWordMap = new HashMap<>();
        try {
            ClassPathResource resource = new ClassPathResource(SENSITIVE_WORD_FILE);
            if (!resource.exists()) {
                log.warn("敏感词库文件不存在，将使用默认敏感词");
                initDefaultWords();
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                Set<String> words = new HashSet<>();
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) { // 忽略空行和注释
                        words.add(line);
                    }
                }
                addSensitiveWordsToHashMap(words);
                log.info("敏感词库加载成功，共 {} 个敏感词", words.size());
            }
        } catch (Exception e) {
            log.error("加载敏感词库失败", e);
            initDefaultWords();
        }
    }

    /**
     * 初始化默认敏感词（如果文件不存在）
     */
    private void initDefaultWords() {
        Set<String> defaultWords = new HashSet<>(Arrays.asList(
                "fuck", "shit", "damn", "bitch", "asshole",
                "操", "草", "日", "滚", "傻逼", "白痴", "垃圾"
        ));
        addSensitiveWordsToHashMap(defaultWords);
        log.info("使用默认敏感词库，共 {} 个敏感词", defaultWords.size());
    }

    /**
     * 将敏感词构建成DFA算法模型
     */
    private void addSensitiveWordsToHashMap(Set<String> sensitiveWords) {
        sensitiveWordMap = new HashMap<>();
        Map<String, Object> nowMap;

        for (String word : sensitiveWords) {
            if (word == null || word.isEmpty()) continue;
            
            nowMap = (Map<String, Object>) sensitiveWordMap;
            char[] chars = word.toCharArray();

            for (int i = 0; i < chars.length; i++) {
                char charWord = chars[i];
                Object tempMap = nowMap.get(String.valueOf(charWord));

                if (tempMap != null) {
                    nowMap = (Map<String, Object>) tempMap;
                } else {
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put(END_FLAG, "0");
                    nowMap.put(String.valueOf(charWord), newMap);
                    nowMap = newMap;
                }

                if (i == chars.length - 1) {
                    nowMap.put(END_FLAG, "1");
                }
            }
        }
    }

    /**
     * 检查文本是否包含敏感词
     * @param text 待检测文本
     * @return true-包含敏感词，false-不包含
     */
    public boolean containsSensitiveWord(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        Map<String, Object> nowMap;
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            nowMap = (Map<String, Object>) sensitiveWordMap;
            int matchFlag = 0;

            for (int j = i; j < chars.length; j++) {
                char word = chars[j];
                Object tempMap = nowMap.get(String.valueOf(word));

                if (tempMap == null) {
                    break;
                }

                nowMap = (Map<String, Object>) tempMap;
                matchFlag++;

                if ("1".equals(nowMap.get(END_FLAG))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取文本中所有敏感词
     * @param text 待检测文本
     * @return 敏感词集合
     */
    public Set<String> getSensitiveWords(String text) {
        Set<String> sensitiveWords = new HashSet<>();
        if (!StringUtils.hasText(text)) {
            return sensitiveWords;
        }

        Map<String, Object> nowMap;
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            nowMap = (Map<String, Object>) sensitiveWordMap;
            int matchFlag = 0;
            StringBuilder wordBuffer = new StringBuilder();

            for (int j = i; j < chars.length; j++) {
                char word = chars[j];
                Object tempMap = nowMap.get(String.valueOf(word));

                if (tempMap == null) {
                    break;
                }

                nowMap = (Map<String, Object>) tempMap;
                matchFlag++;
                wordBuffer.append(word);

                if ("1".equals(nowMap.get(END_FLAG))) {
                    sensitiveWords.add(wordBuffer.toString());
                    break;
                }
            }
        }
        return sensitiveWords;
    }

    /**
     * 替换文本中的敏感词为指定字符
     * @param text 待处理文本
     * @return 处理后的文本
     */
    public String replaceSensitiveWords(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        Set<String> sensitiveWords = getSensitiveWords(text);
        String result = text;

        for (String word : sensitiveWords) {
            String replacement = REPLACEMENT.repeat(word.length());
            result = result.replaceAll(word, replacement);
        }

        return result;
    }

    /**
     * 获取文本中敏感词的数量
     * @param text 待检测文本
     * @return 敏感词数量
     */
    public int getSensitiveWordCount(String text) {
        return getSensitiveWords(text).size();
    }
}

