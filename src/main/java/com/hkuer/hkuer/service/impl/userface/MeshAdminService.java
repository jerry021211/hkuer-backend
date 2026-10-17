package com.hkuer.hkuer.service.impl.userface;
import com.hkuer.hkuer.mapper.userface.*;
import com.hkuer.hkuer.entity.VO.MeshPage;
import com.hkuer.hkuer.utils.*;
import com.hkuer.hkuer.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class MeshAdminService {
 @org.springframework.beans.factory.annotation.Autowired(required=false) private MeshNotificationService notifications;
 private final MeshAdminMapper mapper;private final UserMapper users;private final SnowflakeIdGenerator ids;
 private void admin(){var user=users.selectById(UserContext.getCurrentUserId());if(user==null||!Integer.valueOf(1).equals(user.getUsertype()))throw new BusinessException("403","需要管理员权限");}
 private void kind(String type){if(!Set.of("post","answer","review","material","group","course","announcement").contains(type))throw new BusinessException("400","无效内容类型");}
 public MeshPage<Map<String,Object>> list(String type,int page,int size){admin();kind(type);page=Math.max(1,Math.min(page,10000));size=Math.max(1,Math.min(size,50));return new MeshPage<>(mapper.list(type,size,(page-1)*size),mapper.count(type),page,size);}
 @Transactional public void status(String type,long id,int status,String reason){admin();kind(type);if(!Set.of(1,2).contains(status)||reason==null||reason.isBlank()||reason.length()>500)throw new BusinessException("400","状态或处理理由不正确");Integer previous=mapper.status(type,id);if(previous==null)throw new BusinessException("404","内容不存在");mapper.update(type,id,status);mapper.audit(ids.nextId(),UserContext.getCurrentUserId(),type,id,previous,status,reason);if(notifications!=null&&type.equals("post")&&previous!=status)notifications.post(id,UserContext.getCurrentUserId(),"system",status==1?"你的内容已通过审核":"你的内容已被隐藏",reason);}
 public String announcement(String title,String text,boolean pinned,String category){admin();if(title==null||title.isBlank()||title.length()>200||text==null||text.isBlank()||text.length()>10000)throw new BusinessException("400","公告标题或内容不正确");if(category==null||category.isBlank())category="社区公告";if(category.length()>32)throw new BusinessException("400","公告分类最多 32 字");long id=ids.nextId();mapper.announcement(id,title,text,pinned,category);return Long.toString(id);}
}
