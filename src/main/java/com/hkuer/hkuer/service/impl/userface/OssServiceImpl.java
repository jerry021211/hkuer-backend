package com.hkuer.hkuer.service.impl.userface;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.hkuer.hkuer.config.OssConfig;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.service.userface.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/**
 * 阿里云 OSS 文件服务实现
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.cloud-mode", havingValue = "aliyun", matchIfMissing = true)
public class OssServiceImpl implements OssService {

    /** 允许上传的图片格式 */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /** 最大文件大小：5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssConfig ossConfig;

    @org.springframework.beans.factory.annotation.Value("${mesh.dev.root:}")
    private String localRoot;

    @Override
    public String uploadFile(MultipartFile file, String module) {
        if (module == null || !module.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException("400", "上传模块不合法");
        }
        boolean document = "course-material".equals(module);
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("400", "上传文件不能为空");
        }

        // 2. 校验文件大小
        if (file.getSize() > (document ? 20 * 1024 * 1024 : MAX_FILE_SIZE)) {
            throw new BusinessException("400", document ? "资料不能超过 20MB" : "文件大小不能超过 5MB");
        }

        // 3. 校验文件格式
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase(java.util.Locale.ROOT);
        if (document) {
            try (InputStream input = file.getInputStream()) {
                if (!"pdf".equals(extension) || !"%PDF-".equals(new String(input.readNBytes(5), StandardCharsets.US_ASCII))) {
                    throw new BusinessException("400", "请上传 PDF 文件");
                }
            } catch (java.io.IOException e) {
                throw new BusinessException("400", "无法读取 PDF 文件");
            }
        } else if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("400", "仅支持上传 jpg/png/gif/webp 格式的图片");
        }

        // 4. 生成存储路径：module/yyyy/MM/dd/uuid.ext
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String fileName = UUID.randomUUID().toString(true) + "." + extension;
        String objectKey = module + "/" + datePath + "/" + fileName;

        // 5. 上传文件到 OSS
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(document ? "application/pdf" : switch (extension) {
                case "jpg", "jpeg" -> "image/jpeg";
                default -> "image/" + extension;
            });
            ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream, metadata);
        } catch (Exception e) {
            throw new BusinessException("500", "文件上传失败：" + e.getMessage());
        }

        // 6. 拼接并返回完整的 HTTPS 访问链接
        String url = objectUrlPrefix() + objectKey;
        return url;
    }

    @Override
    public void deleteFile(String url) {
        if (StrUtil.isBlank(url)) {
            return;
        }

        // Existing development uploads remain readable and can still be deleted by their owner.
        if (url.startsWith("/java/media/")) {
            if (StrUtil.isBlank(localRoot) || !url.matches("/java/media/[a-f0-9-]+\\.(jpg|png|gif|pdf)")) {
                throw new BusinessException("400", "无效的历史文件地址");
            }
            try { Files.deleteIfExists(Path.of(localRoot, "uploads", url.substring(url.lastIndexOf('/') + 1))); }
            catch (java.io.IOException e) { throw new BusinessException("500", "删除历史文件失败"); }
            return;
        }

        // 从 URL 中解析出 ObjectKey
        // URL 格式：https://bucketName.endpoint/objectKey
        String host = objectUrlPrefix();
        if (!url.startsWith(host)) {
            throw new BusinessException("400", "文件 URL 格式不正确");
        }

        String objectKey = url.substring(host.length());
        if (StrUtil.isBlank(objectKey)) {
            throw new BusinessException("400", "无法解析文件路径");
        }

        // 删除文件
        ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
    }

    private String objectUrlPrefix() {
        String endpoint = ossConfig.getEndpoint().replaceFirst("^https?://", "").replaceAll("/+$", "");
        return "https://" + ossConfig.getBucketName() + "." + endpoint + "/";
    }

    @Override public boolean isManagedUrl(String url) {
        return OssService.super.isManagedUrl(url) || (url != null && url.startsWith(objectUrlPrefix()));
    }

    /**
     * 获取文件扩展名（不含点号）
     */
    private String getFileExtension(String filename) {
        if (StrUtil.isBlank(filename) || !filename.contains(".")) {
            throw new BusinessException("400", "文件名不合法");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
