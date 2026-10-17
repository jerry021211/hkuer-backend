package com.hkuer.hkuer.service.userface;

public interface AliyunMailService {
    /**
     * 发送邮件
     * @param toAddress 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 是否发送成功
     */
    boolean sendEmail(String toAddress, String subject, String content);
}

