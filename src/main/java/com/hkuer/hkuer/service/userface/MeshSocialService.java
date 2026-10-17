package com.hkuer.hkuer.service.userface;
import java.util.*;
import com.hkuer.hkuer.entity.VO.MeshPage;
public interface MeshSocialService {
 Map<String,Object> profile(String id);void updateProfile(Map<String,Object> profile);
 MeshPage<Map<String,Object>> profileContent(String id,String section,String filter,int current,int size);
 List<Map<String,Object>> conversations();String conversation(String peer,long context);
 List<Map<String,Object>> messages(long id,long before);String message(long id,String content,String clientId);void read(long id,long lastId);
 MeshPage<Map<String,Object>> notifications(String kind,int current,int size);void readNotification(long id);
}
