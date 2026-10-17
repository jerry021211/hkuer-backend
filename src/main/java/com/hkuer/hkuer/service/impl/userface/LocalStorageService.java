package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.service.userface.OssService;
import com.hkuer.hkuer.exception.BusinessException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

@Service @Profile("mesh")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mesh.cloud-mode", havingValue = "local")
public class LocalStorageService implements OssService {
    @Value("${mesh.dev.root}") private String root;
    public String uploadFile(MultipartFile file,String module) {
        boolean document="course-material".equals(module);
        if(file==null || file.isEmpty() || file.getSize()>(document?20:5)*1024*1024) throw new BusinessException("400","图片不能为空且不能超过 5MB");
        try {
            String type=Objects.toString(file.getContentType(),"");
            String extension=document?"pdf":switch(type) {case "image/jpeg" -> "jpg"; case "image/png" -> "png"; case "image/gif" -> "gif"; default -> throw new BusinessException("400","请上传 JPG、PNG 或 GIF 图片");};
            try(var stream=file.getInputStream()) {if(document?!"%PDF-".equals(new String(stream.readNBytes(5),java.nio.charset.StandardCharsets.US_ASCII)):ImageIO.read(stream)==null) throw new BusinessException("400","无效的图片内容");}
            Path directory=Path.of(root,"uploads"); Files.createDirectories(directory);
            String name=UUID.randomUUID()+"."+extension;
            try(var stream=file.getInputStream()){Files.copy(stream,directory.resolve(name));}
            return "/java/media/"+name;
        } catch(java.io.IOException e) {throw new BusinessException("500","保存图片失败");}
    }
    public void deleteFile(String url) {
        if(url==null || !url.matches("/java/media/[a-f0-9-]+\\.(jpg|png|gif|pdf)")) throw new BusinessException("400","无效文件地址");
        try {Files.deleteIfExists(Path.of(root,"uploads",url.substring(url.lastIndexOf('/')+1)));}
        catch(java.io.IOException e){throw new BusinessException("500","删除文件失败");}
    }
}
