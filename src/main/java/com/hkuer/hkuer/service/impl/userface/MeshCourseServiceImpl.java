package com.hkuer.hkuer.service.impl.userface;
import com.hkuer.hkuer.mapper.userface.MeshCourseMapper;
import com.hkuer.hkuer.service.userface.*;
import com.hkuer.hkuer.entity.VO.MeshPage;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.utils.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import java.util.*;

@Service @RequiredArgsConstructor
public class MeshCourseServiceImpl implements MeshCourseService {
 private final com.fasterxml.jackson.databind.ObjectMapper json;
 private final OssService storage;
 private final com.hkuer.hkuer.mapper.userface.AttachmentMapper attachments;private final MeshCourseMapper mapper;private final SnowflakeIdGenerator ids;private final MuteService mute;private final ContentSecurityService security;
 private String actor(){String user=UserContext.getCurrentUserId();if(user==null)throw new BusinessException("401","请先登录");if(mute.isMuted(user))throw new BusinessException("403","禁言期间暂不能操作");return user;}
 private Map<String,Object> course(long id){var c=mapper.detail(id,UserContext.getCurrentUserId());if(c==null)throw new BusinessException("404","课程不存在");return c;}
 public MeshPage<Map<String,Object>> list(String code,String name,String keyword,String level,String subject,boolean mine,String sort,int current,int size){current=Math.max(1,Math.min(10000,current));size=Math.max(1,Math.min(50,size));String user=UserContext.getCurrentUserId();return new MeshPage<>(mapper.list(code,name,keyword,level,subject,mine,user,sort,size,(current-1)*size),mapper.count(code,name,keyword,level,subject,mine,user),current,size);}
 public Map<String,Object> detail(long id){var c=course(id);c.put("reviews",mapper.reviews(id));c.put("voteNotes",mapper.voteNotes(id));c.put("materials",mapper.materials(id));var groups=mapper.groups(id,UserContext.getCurrentUserId());groups.forEach(g->g.put("skills",readSkills(g.get("skills"))));c.put("groups",groups);return c;}
 private List<String> readSkills(Object value){if(value==null)return List.of();try{return json.readValue(value.toString(),new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});}catch(Exception e){return List.of();}}
 private void saveGroupMetadata(long id,Map<String,Object> body,Map<String,Object> existing){
  Object value=body.getOrDefault("skills",readSkills(existing.get("skills")));
  if(!(value instanceof List<?> skills)||skills.size()>8||skills.stream().anyMatch(s->!(s instanceof String)||s.toString().isBlank()||s.toString().length()>20))throw new BusinessException("400","最多 8 个技能标签，每个 20 字以内");
  String wechat=body.containsKey("wechat")?text(body,"wechat",50,false):Objects.toString(existing.get("contact_wechat"),"");
  String note=body.containsKey("contactNote")?text(body,"contactNote",200,false):Objects.toString(existing.get("contact_note"),"");
  try{mapper.groupMetadata(id,json.writeValueAsString(skills.stream().map(Object::toString).map(String::trim).distinct().toList()),wechat,note);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
 }
 public List<Map<String,Object>> subjects(String level){return mapper.subjects(level);}
 private Map<String,Object> groupForMember(long id,String user,boolean ownerOnly){
  var g=mapper.lockGroup(id);if(g==null)throw new BusinessException("404","小组不存在或已解散");
  course(((Number)g.get("course_id")).longValue());
  if(ownerOnly?!user.equals(g.get("owner_id")):mapper.groupMember(id,user)==0)throw new BusinessException("403",ownerOnly?"仅组长可以管理小组":"仅小组成员可以查看成员列表");
  return g;
 }
 @Transactional public Map<String,Object> groupDetail(long id){String user=actor();var g=groupForMember(id,user,false);return Map.of("id",Long.toString(id),"name",g.get("name"),"description",Objects.toString(g.get("description"),""),"capacity",g.get("capacity"),"own",user.equals(g.get("owner_id")),"members",mapper.memberList(id),"skills",readSkills(g.get("skills")),"wechat",Objects.toString(g.get("contact_wechat"),""),"contactNote",Objects.toString(g.get("contact_note"),""));}
 @Transactional public void updateGroup(long id,Map<String,Object> body){
  var existing=groupForMember(id,actor(),true);Object value=body.get("capacity");
  if(!(value instanceof Number n)||n.doubleValue()!=n.intValue()||n.intValue()<2||n.intValue()>100)throw new BusinessException("400","人数需为 2–100 的整数");
  int capacity=((Number)value).intValue();if(capacity<mapper.members(id))throw new BusinessException("409","人数上限不能少于已有成员数");
  String name=text(body,"name",100,true),description=text(body,"description",500,false);security.checkText(name+" "+description);mapper.updateGroup(id,name,description,capacity);saveGroupMetadata(id,body,existing);
 }
 @Transactional public void removeMember(long id,String member){String user=actor();groupForMember(id,user,true);if(user.equals(member))throw new BusinessException("400","不能移除组长，请使用解散小组");mapper.leaveGroup(id,member);}
 @Transactional public void closeGroup(long id){groupForMember(id,actor(),true);mapper.closeGroup(id);}
 @Transactional public void action(long id,String kind,boolean enabled){String user=actor();course(id);switch(kind){case "join" -> {if(enabled)mapper.join(id,user);else mapper.leave(id,user);}case "bookmark" -> {if(enabled)mapper.bookmark(id,user);else mapper.unbookmark(id,user);}case "vote" -> mapper.vote(id,user,enabled);default -> throw new BusinessException("400","不支持的操作");}}
 @Transactional public void vote(long id,boolean recommended,Integer rating,String comment){String user=actor();course(id);if(rating!=null&&(rating<1||rating>5))throw new BusinessException("400","评分范围为 1–5");String note=Objects.toString(comment,"").trim();if(note.length()>280)throw new BusinessException("400","评价最多 280 字");if(!note.isBlank())security.checkText(note);mapper.vote(id,user,recommended);mapper.voteRating(id,user,rating);mapper.voteComment(id,user,note);}
 private String text(Map<String,Object> body,String key,int max,boolean required){String s=Objects.toString(body.get(key),"").trim();if((required&&s.isEmpty())||s.length()>max)throw new BusinessException("400",key+" 内容长度不正确");return s;}
 @Transactional public String review(long id,Map<String,Object> body){
  String user=actor();course(id);Map<String,Object> r=new HashMap<>(body);
  r.put("semester",text(body,"semester",32,false));r.put("content",text(body,"content",1000,false));
  for(String key:List.of("overall","difficulty","workload","teaching","examDifficulty")){
   Object value=body.get(key);boolean required=key.equals("overall");
   if(!required&&(value==null||(value instanceof Number n&&n.doubleValue()==0))){r.put(key,null);continue;}
   if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()!=n.intValue()||n.intValue()<1||n.intValue()>5)throw new BusinessException("400",required?"请选择总体评分（1–5 星）":"评分范围为 1–5 星");
   r.put(key,((Number)value).intValue());
  }
  for(String key:List.of("midterm","finalExam","openBook")){
   Object value=body.get(key);if(value!=null&&!(value instanceof Boolean))throw new BusinessException("400","考试信息格式不正确");r.put(key,value);
  }
  r.put("anonymous",Boolean.TRUE.equals(body.get("anonymous")));
  if(!r.get("content").toString().isBlank())security.checkText(r.get("content").toString());
  long reviewId=ids.nextId();try{mapper.review(reviewId,id,user,r);}catch(DuplicateKeyException e){throw new BusinessException("409","本学期已评价过该课程");}mapper.join(id,user);return Long.toString(reviewId);
 }
 @Transactional public String group(long id,Map<String,Object> body){String user=actor();course(id);if(mapper.joined(id,user)==0)throw new BusinessException("403","请先参与课程再创建小组");Object value=body.getOrDefault("capacity",4);if(!(value instanceof Number n)||n.doubleValue()!=n.intValue()||n.intValue()<2||n.intValue()>100)throw new BusinessException("400","人数需为 2–100 的整数");int capacity=((Number)value).intValue();String name=text(body,"name",100,true),description=text(body,"description",500,false);security.checkText(name+" "+description);long group=ids.nextId();mapper.group(group,id,user,name,description,capacity);saveGroupMetadata(group,body,Map.of());mapper.joinGroup(group,user);return Long.toString(group);}
 @Transactional public void joinGroup(long id,boolean enabled){String user=actor();var g=mapper.lockGroup(id);if(g==null)throw new BusinessException("404","小组不存在");long courseId=((Number)g.get("course_id")).longValue();course(courseId);if(enabled&&mapper.joined(courseId,user)==0)throw new BusinessException("403","请先参与课程");if(enabled){if(mapper.groupMember(id,user)>0)return;if(mapper.members(id)>=((Number)g.get("capacity")).intValue())throw new BusinessException("409","小组人数已满");mapper.joinGroup(id,user);}else{if(user.equals(g.get("owner_id")))throw new BusinessException("400","组长不能退出自己创建的小组");mapper.leaveGroup(id,user);}}
 @Transactional public String material(long id,Map<String,Object> body){String user=actor();course(id);if(mapper.joined(id,user)==0)throw new BusinessException("403","请先参与课程再分享资料");String category=text(body,"category",32,true);if(!Set.of("pastpaper","slides","openbook","notes","other").contains(category))throw new BusinessException("400","资料类别不正确");String url=text(body,"url",1024,true);if(!url.startsWith("/java/media/")&&!url.startsWith("https://"))throw new BusinessException("400","无效资料地址");if(storage.isManagedUrl(url)&&attachments.owned(url,user)==0)throw new BusinessException("403","请使用自己上传的资料");long material=ids.nextId();mapper.material(material,id,user,text(body,"title",200,true),category,url);return Long.toString(material);}
 @Transactional public String create(Map<String,Object> body){actor();Map<String,Object> r=new HashMap<>(body);for(String key:List.of("code","name","level","subject"))r.put(key,text(body,key,key.equals("name")?200:64,true));if(!Set.of("UG","PG").contains(r.get("level")))throw new BusinessException("400","课程级别应为 UG 或 PG");r.putIfAbsent("subtitle","");r.putIfAbsent("description","");r.putIfAbsent("credits",6);long id=ids.nextId();mapper.create(id,r);return Long.toString(id);}
}
