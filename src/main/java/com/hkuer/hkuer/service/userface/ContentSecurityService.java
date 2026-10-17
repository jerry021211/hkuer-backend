package com.hkuer.hkuer.service.userface;

/**
 * 内容安全服务接口
 */
public interface ContentSecurityService {

    /**
     * 检测文本内容是否安全
     *
     * @param content 待检测的文本内容
     * @return true-安全，false-不安全（会抛出异常，正常情况下不会返回false）
     * @throws com.hkuer.hkuer.exception.ContentRiskException 如果检测到违规内容
     */
    boolean checkText(String content);
}
