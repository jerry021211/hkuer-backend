package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.userface.AttachmentService;
import com.hkuer.hkuer.service.userface.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private OssService ossService;

    @Autowired
    private AttachmentService attachmentService;
    @Autowired(required=false)
    private com.hkuer.hkuer.config.MeshFilePolicy policy;

    /**
     * 上传文件（图片）
     * 上传成功后，在 attachment 表中记录一条 status=0（临时）的记录。
     * 只有当帖子正式发布时，才会将引用的图片转为 status=1（正式）。
     *
     * @param file   上传的文件
     * @param module 模块名，默认 "common"（可选值如 avatar、post 等）
     * @return 文件的完整 HTTPS 访问链接
     */
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "module", defaultValue = "common") String module) {
        if(policy!=null)policy.beforeUpload(module);
        // 1. 上传到 OSS
        String url = ossService.uploadFile(file, module);
        // 2. 写入 attachment 表，状态为 PENDING(0)
        attachmentService.savePending(url);
        if(policy!=null)policy.bind(url,file);
        return Result.success(url);
    }

    /**
     * 删除文件
     *
     * @param url 文件的完整访问链接
     */
    @PostMapping("/delete")
    public Result delete(@RequestParam("url") String url) {
        if(policy!=null)policy.assertDelete(url);
        ossService.deleteFile(url);
        return Result.success();
    }
}
