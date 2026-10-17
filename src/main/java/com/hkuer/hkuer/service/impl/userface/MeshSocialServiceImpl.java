package com.hkuer.hkuer.service.impl.userface;
import com.hkuer.hkuer.mapper.userface.*;
import com.hkuer.hkuer.service.userface.*;
import com.hkuer.hkuer.entity.DTO.UpdateProfileRequest;
import com.hkuer.hkuer.entity.VO.MeshPage;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.utils.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class MeshSocialServiceImpl implements MeshSocialService {
 private final MeshSocialMapper mapper;private final MeshAccountMapper account;private final UserMapper users;private final MeshContentMapper content;private final UserService userService;private final MuteService mute;private final SnowflakeIdGenerator ids;private final ObjectMapper json;private final ContentSecurityService security;
 private String actor(){String user=UserContext.getCurrentUserId();if(user==null)throw new BusinessException("401","请先登录");return user;}
 private String writer(){String user=actor();if(mute.isMuted(user))throw new BusinessException("403","禁言期间暂不能发送内容");return user;}
 public MeshPage<Map<String,Object>> profileContent(String id,String section,String filter,int current,int size){
  if(!Set.of("public","posts","answers","reviews","watched","market","rental","market-favorites","bookmarks").contains(section))throw new BusinessException("400","未知内容分类");
  if(mapper.profile(id)==null)throw new BusinessException("404","用户不存在");
  boolean own=id.equals(UserContext.getCurrentUserId());
  if(!own&&!Set.of("public","posts","answers","reviews").contains(section))throw new BusinessException("403","无权查看私人内容");
  current=Math.max(1,Math.min(10000,current));size=Math.max(1,Math.min(50,size));
  return new MeshPage<>(mapper.profileContent(id,own,section,filter,size,(current-1)*size),mapper.profileContentCount(id,own,section,filter),current,size);
 }
 public Map<String,Object> profile(String id){var profile=mapper.profile(id);if(profile==null)throw new BusinessException("404","用户不存在");boolean own=id.equals(UserContext.getCurrentUserId());profile.put("own",own);profile.put("stats",mapper.stats(id,own));Map<String,Long> counts=new LinkedHashMap<>();for(String kind:List.of("qa","job","life"))counts.put(kind,mapper.profileContentCount(id,own,"posts",kind));profile.put("postCounts",counts);try{profile.put("skills",json.readValue(Objects.toString(profile.get("skills"),"[]"),List.class));}catch(Exception e){profile.put("skills",List.of());}profile.put("badges",account.badges(id));profile.put("posts",mapper.posts(id,own));profile.put("answers",mapper.answers(id,own));if(own){profile.put("favorites",mapper.favorites(id));profile.put("bookmarkedCourses",mapper.bookmarkedCourses(id));profile.put("watched",mapper.watched(id));profile.put("groups",mapper.groups(id));profile.put("email",users.selectById(id).getEmail());}return profile;}
 @Transactional public void updateProfile(Map<String,Object> body){String user=writer();String nickname=Objects.toString(body.get("nickname"),"").trim();if(nickname.isEmpty()||nickname.length()>20)throw new BusinessException("400","昵称需为 1–20 字");Map<String,Object> p=new HashMap<>(body);for(String key:List.of("bio","github","linkedin","website","smoking","sleepSchedule","cleanliness","occupation")){String v=Objects.toString(body.get(key),"").trim();int max=key.equals("bio")?100:512;if(v.length()>max)throw new BusinessException("400",key+" 过长");if(List.of("github","linkedin","website").contains(key)&&!v.isEmpty()&&!v.startsWith("https://")&&!v.startsWith("http://"))throw new BusinessException("400","外部链接需要 http(s) 地址");p.put(key,v);}Object skills=body.getOrDefault("skills",List.of());if(!(skills instanceof List<?> list)||list.size()>10||list.stream().anyMatch(v->!(v instanceof String)||v.toString().length()>30))throw new BusinessException("400","最多 10 个技能，每个 30 字以内");var request=new UpdateProfileRequest();request.setNickname(nickname);request.setAvatar(Objects.toString(body.get("avatar"),""));var result=userService.updateProfile(user,request);if(!"200".equals(result.getCode()))throw new BusinessException(result.getCode(),result.getMsg());try{mapper.updateProfile(user,p,json.writeValueAsString(skills));}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}}
 public List<Map<String,Object>> conversations(){return mapper.conversations(actor());}
 @Transactional public String conversation(String peer,long context){String user=writer();if(peer==null||peer.equals(user)||mapper.profile(peer)==null)throw new BusinessException("400","请选择其他用户");if(context>0){var item=content.detail(context,user,false);if(item==null||((Number)item.get("status")).intValue()!=1)throw new BusinessException("404","相关内容不存在");if(!peer.equals(item.get("authorId")))throw new BusinessException("400","会话对象与发布者不一致");}String low=user.compareTo(peer)<0?user:peer,high=user.compareTo(peer)<0?peer:user;String existing=mapper.existing(low,high,context);if(existing!=null)return existing;mapper.conversation(ids.nextId(),low,high,context);String id=mapper.existing(low,high,context);mapper.member(Long.parseLong(id),user);mapper.member(Long.parseLong(id),peer);return id;}
 private String member(long id){String user=actor();if(mapper.isMember(id,user)==0)throw new BusinessException("403","无权访问此会话");return user;}
 public List<Map<String,Object>> messages(long id,long before){member(id);var rows=mapper.messages(id,Math.max(before,0));rows.forEach(row->row.put("readByPeer",((Number)row.get("readByPeer")).intValue()>0));Collections.reverse(rows);return rows;}
 @Transactional public String message(long id,String text,String clientId){String user=member(id);writer();if(text==null||text.isBlank()||text.length()>2000||clientId==null||!clientId.matches("[a-zA-Z0-9-]{8,64}"))throw new BusinessException("400","消息内容或请求标识无效");String existing=mapper.existingMessage(user,clientId,id);if(existing!=null)return existing;security.checkText(text);long messageId=ids.nextId();try{mapper.message(messageId,id,user,text.trim(),clientId);}catch(org.springframework.dao.DuplicateKeyException e){String duplicate=mapper.existingMessage(user,clientId,id);if(duplicate!=null)return duplicate;throw e;}mapper.touch(id);return Long.toString(messageId);}
 @Transactional public void read(long id,long lastId){mapper.read(id,member(id),Math.max(0,lastId));}
 public MeshPage<Map<String,Object>> notifications(String kind,int current,int size){String user=actor();current=Math.max(1,Math.min(10000,current));size=Math.max(1,Math.min(50,size));return new MeshPage<>(mapper.notifications(user,kind,size,(current-1)*size),mapper.notificationCount(user,kind),current,size);}
 public void readNotification(long id){mapper.readNotification(actor(),id);}
}
