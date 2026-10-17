package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.service.userface.AdSensitiveWordService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 广告敏感词检测服务实现类
 * 基于DFA算法，专门用于检测广告类敏感词
 */
@Slf4j
@Service
public class AdSensitiveWordServiceImpl implements AdSensitiveWordService {

    private Map<String, Object> sensitiveWordMap;
    private static final String END_FLAG = "isEnd";

    /**
     * 广告敏感词库
     */
    private static final Set<String> AD_SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "推广", "广告", "促销", "优惠", "折扣", "特价", "限时", "抢购",
            "代购", "代理", "加盟", "招商", "投资", "理财", "贷款", "借款",
            "刷单", "兼职", "招聘", "高薪", "月入", "日赚", "赚钱", "收益",
            "购买", "购买链接", "购买方式", "联系", "微信", "QQ群", "扫码", "二维码", "加微信", "加QQ", "私信", "详情", "了解",
            "产品", "服务", "商品", "店铺", "商城", "官网", "网址", "链接",
            "推广", "转发", "分享", "关注", "点赞", "评论", "领取", "免费",
            "vx", "wx", "wechat", "weixin", "qrcode", "qr code", "buy now",
            "sale", "discount", "promotion", "ad", "advertisement", "sponsor",
            "sponsored", "affiliate", "commission", "referral", "link", "click here"
    ));

    /**
     * 初始化敏感词库
     */
    @PostConstruct
    public void init() {
        sensitiveWordMap = new HashMap<>();
        addSensitiveWordsToHashMap(AD_SENSITIVE_WORDS);
        log.info("广告敏感词库加载成功，共 {} 个敏感词", AD_SENSITIVE_WORDS.size());
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

    @Override
    public boolean containsAdSensitiveWord(String text) {
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

    @Override
    public Set<String> getAdSensitiveWords(String text) {
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
}
