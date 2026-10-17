package com.hkuer.hkuer.service.userface;
import com.hkuer.hkuer.entity.VO.MeshPage;
import java.util.*;
public interface MeshCourseService {
 MeshPage<Map<String,Object>> list(String code,String name,String keyword,String level,String subject,boolean mine,String sort,int current,int size);
 Map<String,Object> detail(long id);
 List<Map<String,Object>> subjects(String level);
 void action(long id,String kind,boolean enabled);
 String review(long id,Map<String,Object> body);
 String group(long id,Map<String,Object> body);
 void joinGroup(long id,boolean enabled);
 Map<String,Object> groupDetail(long id);
 void updateGroup(long id,Map<String,Object> body);
 void removeMember(long id,String member);
 void closeGroup(long id);
 void vote(long id,boolean recommended,Integer rating,String comment);
 String material(long id,Map<String,Object> body);
 String create(Map<String,Object> body);
}
