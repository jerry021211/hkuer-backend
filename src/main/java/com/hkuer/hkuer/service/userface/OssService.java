package com.hkuer.hkuer.service.userface;

import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云 OSS 文件服务接口
 */
public interface OssService {

    /**
     * 上传文件到 OSS
     *
     * @param file   上传的文件
     * @param module 模块名（如 avatar、post），用于分目录存储
     * @return 文件的完整 HTTPS 访问链接
     */
    String uploadFile(MultipartFile file, String module);

    /** Recognizes uploads whose ownership must be checked by the application. */
    default boolean isManagedUrl(String url) {
        return url != null && url.startsWith("/java/media/");
    }

    /**
     * 根据文件 URL 删除 OSS 上的文件
     *
     * @param url 文件的完整访问链接
     */
    void deleteFile(String url);
}
