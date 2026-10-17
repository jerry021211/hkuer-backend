package com.hkuer.hkuer.service.impl.userface;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.hkuer.hkuer.service.userface.AliyunMailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.mail-mode", havingValue = "aliyun", matchIfMissing = true)
public class AliyunMailServiceImpl implements AliyunMailService {

    @Value("${aliyun.mail.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.mail.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.mail.accountName}")
    private String accountName;

    @Value("${aliyun.mail.regionId}")
    private String regionId;

    @Override
    public boolean sendEmail(String toAddress, String subject, String content) {
        try {
            // 创建DefaultAcsClient实例并初始化
            DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
            IAcsClient client = new DefaultAcsClient(profile);

            // 创建API请求并设置参数
            SingleSendMailRequest request = new SingleSendMailRequest();
            // 使用账号名称发送邮件
            request.setAccountName(accountName);
            // 发信人昵称
            request.setFromAlias("HKUER");
            // 目标地址
            request.setToAddress(toAddress);
            request.setAddressType(0);
            request.setReplyToAddress(false);
            // 邮件主题
            request.setSubject(subject);
            // 邮件正文（HTML格式）
            request.setHtmlBody(content);

            // 发送邮件
            SingleSendMailResponse response = client.getAcsResponse(request);
            log.info("阿里云已受理邮件发送请求，RequestId: {}", response.getRequestId());
            return true;
        } catch (Exception e) {
            // 处理阿里云 SDK 异常（使用反射避免直接依赖 ClientException）
            String errorMsg = e.getMessage();
            String className = e.getClass().getName();
            if (className.contains("ClientException") || className.contains("com.aliyuncs.exceptions")) {
                try {
                    java.lang.reflect.Method getErrCode = e.getClass().getMethod("getErrCode");
                    java.lang.reflect.Method getErrMsg = e.getClass().getMethod("getErrMsg");
                    String errCode = (String) getErrCode.invoke(e);
                    String errMsg = (String) getErrMsg.invoke(e);
                    log.error("邮件发送失败，错误码: {}, 错误信息: {}", errCode, errMsg, e);
                } catch (Exception ex) {
                    log.error("邮件发送失败: {}", errorMsg, e);
                }
            } else {
                log.error("邮件发送异常: {}", errorMsg, e);
            }
            return false;
        }
    }
}

