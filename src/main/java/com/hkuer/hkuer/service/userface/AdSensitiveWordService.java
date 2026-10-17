package com.hkuer.hkuer.service.userface;

/**
 * 广告敏感词检测服务接口
 */
public interface AdSensitiveWordService {

    /**
     * 检测文本是否包含广告敏感词
     *
     * @param text 待检测文本
     * @return true-包含广告敏感词，false-不包含
     */
    boolean containsAdSensitiveWord(String text);

    /**
     * 获取文本中所有匹配的广告敏感词
     *
     * @param text 待检测文本
     * @return 匹配的敏感词集合
     */
    java.util.Set<String> getAdSensitiveWords(String text);
}
