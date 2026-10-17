package com.hkuer.hkuer.controller.userface;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.service.userface.AttachmentService;
import com.hkuer.hkuer.service.userface.OssService;
import com.hkuer.hkuer.config.MeshFilePolicy;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import java.nio.file.*;
import java.util.*;
@RestController @Profile("mesh") @RequiredArgsConstructor
public class MeshDocumentController {
 private final AttachmentService attachments;private final MeshFilePolicy policy;private final OssService storage;
 @PostMapping("/mesh/files/upload")public Result upload(@RequestParam MultipartFile file)throws java.io.IOException{
  policy.beforeUpload("course");if(file.isEmpty()||file.getSize()>20*1024*1024)throw new BusinessException("400","资料需为 20MB 以内的 PDF");
  try(var in=file.getInputStream()){if(!new String(in.readNBytes(5),java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))throw new BusinessException("400","请上传 PDF 文件");}
  String url=storage.uploadFile(file,"course-material");attachments.savePending(url);policy.bind(url,file);return Result.success(url);
 }
}
