package com.hkuer.hkuer.mapper.userface;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface MeshSocialMapper {
 String PROFILE_CONTENT="""
 <choose>
  <when test="section == 'reviews'">
   SELECT CAST(c.id AS CHAR) id,CONCAT(c.code,' · ',c.name,' — ',r.content) title,'course' kind,r.created_at createdAt
   FROM course_review r JOIN course c ON c.id=r.course_id
   WHERE r.user_id=#{user} AND r.status=1 AND c.status=1 AND (r.anonymous=false OR #{own})
  </when>
  <when test="section == 'answers'">
   SELECT CAST(a.question_id AS CHAR) id,a.content title,'qa' kind,a.created_at createdAt
   FROM answer a JOIN post p ON p.id=a.question_id
   WHERE a.author_id=#{user} AND a.status=1 AND p.status=1 AND (a.anonymous=false OR #{own})
  </when>
  <when test="section == 'bookmarks'">
   SELECT CAST(p.id AS CHAR) id,p.title,m.kind,f.created_at createdAt
   FROM post p JOIN post_meta m ON m.post_id=p.id JOIN post_favorite f ON f.target_id=p.id
   WHERE f.user_id=#{user} AND p.status=1 AND m.kind!='market' AND (#{filter}='' OR m.kind=#{filter})
   UNION ALL
   SELECT CAST(c.id AS CHAR) id,c.name title,'course' kind,b.created_at createdAt
   FROM course_bookmark b JOIN course c ON c.id=b.course_id
   WHERE b.user_id=#{user} AND c.status=1 AND (#{filter}='' OR #{filter}='course')
  </when>
  <when test="section == 'market-favorites'">
   SELECT CAST(p.id AS CHAR) id,p.title,m.kind,f.created_at createdAt
   FROM post p JOIN post_meta m ON m.post_id=p.id JOIN post_favorite f ON f.target_id=p.id
   WHERE f.user_id=#{user} AND p.status=1 AND m.kind='market'
  </when>
  <when test="section == 'watched'">
   SELECT CAST(p.id AS CHAR) id,p.title,m.kind,q.created_at createdAt
   FROM post p JOIN post_meta m ON m.post_id=p.id JOIN question_interaction q ON q.question_id=p.id
   WHERE q.user_id=#{user} AND q.kind='watch' AND p.status=1
  </when>
  <otherwise>
   SELECT CAST(p.id AS CHAR) id,p.title,m.kind,p.created_at createdAt
   FROM post p JOIN post_meta m ON m.post_id=p.id
   LEFT JOIN market_item i ON i.post_id=p.id LEFT JOIN rental_post r ON r.post_id=p.id
   WHERE p.author_id=#{user} AND (#{own} OR (m.anonymous=false AND p.status=1))
   <choose>
    <when test="section == 'market'">AND m.kind='market' AND (#{filter}='' OR i.sale_status=#{filter})</when>
    <when test="section == 'rental'">AND m.kind='rental' AND (#{filter}='' OR r.rental_type=#{filter})</when>
    <when test="section == 'posts'">AND m.kind NOT IN ('market','rental') AND (#{filter}='' OR m.kind=#{filter})</when>
   </choose>
  </otherwise>
 </choose>
 """;
 @Select("<script>SELECT * FROM ("+PROFILE_CONTENT+") collection ORDER BY createdAt DESC,id DESC LIMIT #{size} OFFSET #{offset}</script>")
 List<Map<String,Object>> profileContent(@Param("user")String user,@Param("own")boolean own,@Param("section")String section,@Param("filter")String filter,@Param("size")int size,@Param("offset")int offset);
 @Select("<script>SELECT COUNT(*) FROM ("+PROFILE_CONTENT+") collection</script>")
 long profileContentCount(@Param("user")String user,@Param("own")boolean own,@Param("section")String section,@Param("filter")String filter);
 @Select("SELECT (SELECT COUNT(*) FROM post p JOIN post_meta m ON m.post_id=p.id WHERE p.author_id=#{user} AND p.status=1 AND (m.anonymous=false OR #{own})) posts,(SELECT COUNT(*) FROM answer a JOIN post p ON p.id=a.question_id WHERE a.author_id=#{user} AND a.status=1 AND p.status=1 AND (a.anonymous=false OR #{own})) answers,(SELECT COUNT(*) FROM answer a JOIN post p ON p.id=a.question_id WHERE a.author_id=#{user} AND a.status=1 AND p.status=1 AND a.accepted=true AND (a.anonymous=false OR #{own})) accepted,(SELECT COUNT(*) FROM course_review WHERE user_id=#{user} AND status=1 AND (anonymous=false OR #{own})) reviews,(SELECT COALESCE(SUM(p.like_num),0) FROM post p JOIN post_meta m ON m.post_id=p.id WHERE p.author_id=#{user} AND p.status=1 AND (m.anonymous=false OR #{own})) likes") Map<String,Object> stats(@Param("user")String user,@Param("own")boolean own);
 @Select("SELECT CAST(c.id AS CHAR) id,c.code,c.name title,'course' kind,b.created_at createdAt FROM course_bookmark b JOIN course c ON c.id=b.course_id WHERE b.user_id=#{user} AND c.status=1 ORDER BY b.created_at DESC LIMIT 100") List<Map<String,Object>> bookmarkedCourses(String user);

 @Select("SELECT u.id userId,u.nickname,u.avatar,u.created_at createdAt,p.bio,p.skills,p.github,p.linkedin,p.website,p.verified_hku verifiedHku,p.active_badge activeBadge,p.smoking,p.sleep_schedule sleepSchedule,p.cleanliness,p.occupation FROM user u LEFT JOIN user_profile p ON p.user_id=u.id WHERE u.id=#{id} AND u.password IS NOT NULL") Map<String,Object> profile(String id);
 @Insert("INSERT INTO user_profile(user_id,bio,skills,github,linkedin,website,smoking,sleep_schedule,cleanliness,occupation) VALUES(#{id},#{p.bio},#{skills},#{p.github},#{p.linkedin},#{p.website},#{p.smoking},#{p.sleepSchedule},#{p.cleanliness},#{p.occupation}) ON DUPLICATE KEY UPDATE bio=#{p.bio},skills=#{skills},github=#{p.github},linkedin=#{p.linkedin},website=#{p.website},smoking=#{p.smoking},sleep_schedule=#{p.sleepSchedule},cleanliness=#{p.cleanliness},occupation=#{p.occupation}") void updateProfile(@Param("id")String id,@Param("p")Map<String,Object> profile,@Param("skills")String skills);
 @Select("SELECT CAST(c.id AS CHAR) id,CAST(c.context_post_id AS CHAR) contextPostId,p.title contextTitle,pm.kind contextKind,JSON_UNQUOTE(JSON_EXTRACT(pm.images,'$[0]')) contextImage,mi.price contextPrice,u.id peerId,u.nickname peerName,u.avatar peerAvatar,(SELECT content FROM chat_message msg WHERE msg.conversation_id=c.id ORDER BY msg.id DESC LIMIT 1) lastMessage,(SELECT COUNT(*) FROM chat_message msg WHERE msg.conversation_id=c.id AND msg.id>m.last_read_id AND msg.sender_id!=#{user}) unread,c.updated_at updatedAt FROM conversation c JOIN conversation_member m ON m.conversation_id=c.id AND m.user_id=#{user} JOIN user u ON u.id=IF(c.user_low=#{user},c.user_high,c.user_low) LEFT JOIN post p ON p.id=c.context_post_id LEFT JOIN post_meta pm ON pm.post_id=p.id LEFT JOIN market_item mi ON mi.post_id=p.id ORDER BY c.updated_at DESC") List<Map<String,Object>> conversations(String user);
 @Select("SELECT CAST(id AS CHAR) FROM conversation WHERE user_low=#{low} AND user_high=#{high} AND context_post_id=#{context}") String existing(@Param("low")String low,@Param("high")String high,@Param("context")long context);
 @Insert("INSERT IGNORE INTO conversation(id,user_low,user_high,context_post_id) VALUES(#{id},#{low},#{high},#{context})") void conversation(@Param("id")long id,@Param("low")String low,@Param("high")String high,@Param("context")long context);
 @Insert("INSERT IGNORE INTO conversation_member(conversation_id,user_id) VALUES(#{id},#{user})") void member(@Param("id")long id,@Param("user")String user);
 @Select("SELECT COUNT(*) FROM conversation_member WHERE conversation_id=#{id} AND user_id=#{user}") int isMember(@Param("id")long id,@Param("user")String user);
 @Select("<script>SELECT CAST(id AS CHAR) id,sender_id senderId,content,created_at createdAt,EXISTS(SELECT 1 FROM conversation_member cm WHERE cm.conversation_id=chat_message.conversation_id AND cm.user_id!=chat_message.sender_id AND cm.last_read_id>=chat_message.id) readByPeer FROM chat_message WHERE conversation_id=#{id}<if test=\"before &gt; 0\"> AND id &lt; #{before}</if> ORDER BY id DESC LIMIT 50</script>") List<Map<String,Object>> messages(@Param("id")long id,@Param("before")long before);
 @Select("SELECT CAST(id AS CHAR) FROM chat_message WHERE sender_id=#{user} AND client_id=#{clientId} AND conversation_id=#{conversation}") String existingMessage(@Param("user")String user,@Param("clientId")String clientId,@Param("conversation")long conversation);
 @Insert("INSERT INTO chat_message(id,conversation_id,sender_id,content,client_id) VALUES(#{id},#{conversation},#{user},#{content},#{clientId})") void message(@Param("id")long id,@Param("conversation")long conversation,@Param("user")String user,@Param("content")String content,@Param("clientId")String clientId);
 @Update("UPDATE conversation SET updated_at=NOW() WHERE id=#{id}") void touch(long id);
 @Update("UPDATE conversation_member SET last_read_id=GREATEST(last_read_id,COALESCE((SELECT MAX(id) FROM chat_message WHERE conversation_id=#{id} AND (#{lastId}=0 OR id <= #{lastId})),0)) WHERE conversation_id=#{id} AND user_id=#{user}") void read(@Param("id")long id,@Param("user")String user,@Param("lastId")long lastId);
 @Select("SELECT CAST(id AS CHAR) id,kind,title,content,target_path targetPath,is_read isRead,created_at createdAt FROM notification WHERE user_id=#{user} AND ((#{kind}='system' AND kind='system') OR (#{kind}!='system' AND kind!='system')) ORDER BY id DESC LIMIT #{size} OFFSET #{offset}") List<Map<String,Object>> notifications(@Param("user")String user,@Param("kind")String kind,@Param("size")int size,@Param("offset")int offset);
 @Select("SELECT COUNT(*) FROM notification WHERE user_id=#{user} AND ((#{kind}='system' AND kind='system') OR (#{kind}!='system' AND kind!='system'))") long notificationCount(@Param("user")String user,@Param("kind")String kind);
 @Update("UPDATE notification SET is_read=true WHERE user_id=#{user} AND (#{id}=0 OR id=#{id})") void readNotification(@Param("user")String user,@Param("id")long id);
 @Select("SELECT CAST(a.id AS CHAR) id,CAST(a.question_id AS CHAR) questionId,p.title,a.content,a.created_at createdAt FROM answer a JOIN post p ON p.id=a.question_id WHERE a.author_id=#{user} AND a.status=1 AND p.status=1 AND (a.anonymous=false OR #{own}) ORDER BY a.id DESC LIMIT 100") List<Map<String,Object>> answers(@Param("user")String user,@Param("own")boolean own);
 @Select("SELECT CAST(p.id AS CHAR) id,p.title,m.kind,p.created_at createdAt FROM post p JOIN post_meta m ON m.post_id=p.id JOIN post_favorite f ON f.target_id=p.id WHERE f.user_id=#{user} AND p.status=1 ORDER BY f.created_at DESC LIMIT 100") List<Map<String,Object>> favorites(String user);
 @Select("SELECT CAST(p.id AS CHAR) id,p.title,m.kind,p.created_at createdAt FROM post p JOIN post_meta m ON m.post_id=p.id JOIN question_interaction q ON q.question_id=p.id WHERE q.user_id=#{user} AND q.kind='watch' AND p.status=1 ORDER BY q.created_at DESC LIMIT 100") List<Map<String,Object>> watched(String user);
 @Select("SELECT CAST(p.id AS CHAR) id,p.title,m.kind,p.status,p.created_at createdAt,(SELECT sale_status FROM market_item i WHERE i.post_id=p.id) saleStatus,(SELECT rental_type FROM rental_post r WHERE r.post_id=p.id) rentalType,(SELECT listing_status FROM rental_post r WHERE r.post_id=p.id) listingStatus FROM post p JOIN post_meta m ON m.post_id=p.id WHERE p.author_id=#{user} AND (#{own} OR (m.anonymous=false AND p.status=1)) ORDER BY p.created_at DESC LIMIT 100") List<Map<String,Object>> posts(@Param("user")String user,@Param("own")boolean own);
 @Select("SELECT CAST(g.id AS CHAR) id,g.name,CAST(g.course_id AS CHAR) courseId FROM course_group g JOIN course_group_member m ON m.group_id=g.id WHERE m.user_id=#{user} AND g.status=1") List<Map<String,Object>> groups(String user);
}
