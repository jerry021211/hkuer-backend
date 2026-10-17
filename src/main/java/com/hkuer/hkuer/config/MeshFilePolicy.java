package com.hkuer.hkuer.config;
import com.hkuer.hkuer.mapper.userface.AttachmentMapper;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.utils.UserContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
@Component @Profile("mesh") @RequiredArgsConstructor
public class MeshFilePolicy {
 private final AttachmentMapper files;
 public void beforeUpload(String module){if(!"avatar".equals(module)&&UserContext.getCurrentUserId()==null)throw new BusinessException("401","请先登录再上传");}
 public void bind(String url,MultipartFile file){files.bindOwner(url,UserContext.getCurrentUserId(),file.getOriginalFilename(),file.getContentType(),file.getSize());}
 public void assertDelete(String url){String user=UserContext.getCurrentUserId();if(user==null||files.pendingOwned(url,user)==0)throw new BusinessException("403","仅可删除自己未使用的附件");}
}
