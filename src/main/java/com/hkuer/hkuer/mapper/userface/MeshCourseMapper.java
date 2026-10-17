package com.hkuer.hkuer.mapper.userface;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface MeshCourseMapper {
 @Select("SELECT COUNT(*) FROM course_group_member WHERE group_id=#{id} AND user_id=#{user}") int groupMember(@Param("id")long id,@Param("user")String user);
 @Update("UPDATE course_vote SET rating=#{rating} WHERE course_id=#{id} AND user_id=#{user}") void voteRating(@Param("id")long id,@Param("user")String user,@Param("rating")Integer rating);
 @Update("UPDATE course_vote SET comment=#{comment} WHERE course_id=#{id} AND user_id=#{user}") void voteComment(@Param("id")long id,@Param("user")String user,@Param("comment")String comment);
 @Select("SELECT v.rating,v.comment content,u.nickname authorName FROM course_vote v JOIN user u ON u.id=v.user_id WHERE v.course_id=#{id} AND (v.comment<>'' OR v.rating IS NOT NULL) ORDER BY v.created_at DESC,v.user_id LIMIT 20") List<Map<String,Object>> voteNotes(long id);

 String COLUMNS="""
 SELECT CAST(c.id AS CHAR) id,c.code,c.name,c.subtitle,c.level,c.subject,c.credits,c.description,
 COALESCE((SELECT AVG(overall) FROM course_review r WHERE r.course_id=c.id AND r.status=1),0) rating,
 (SELECT COUNT(*) FROM course_review r WHERE r.course_id=c.id AND r.status=1) reviewCount,
 (SELECT COUNT(*) FROM course_participation p WHERE p.course_id=c.id) participantCount,
 EXISTS(SELECT 1 FROM course_participation p WHERE p.course_id=c.id AND p.user_id=#{user}) participating,
 (SELECT COUNT(*) FROM course_vote v WHERE v.course_id=c.id AND v.recommend=true) recommendCount,
 (SELECT COUNT(*) FROM course_vote v WHERE v.course_id=c.id AND v.recommend=false) dislikeCount,
 EXISTS(SELECT 1 FROM course_bookmark b WHERE b.course_id=c.id AND b.user_id=#{user}) bookmarked
 FROM course c
 """;
 String FILTER="""
 WHERE c.status=1
 <if test="keyword != ''">AND (c.code LIKE CONCAT('%',#{keyword},'%') OR c.name LIKE CONCAT('%',#{keyword},'%') OR c.subtitle LIKE CONCAT('%',#{keyword},'%'))</if>
 <if test="code != ''">AND c.code LIKE CONCAT('%',#{code},'%')</if>
 <if test="name != ''">AND c.name LIKE CONCAT('%',#{name},'%')</if>
 <if test="level != ''">AND c.level=#{level}</if>
 <if test="subject != ''">AND c.subject=#{subject}</if>
 <if test="mine">AND EXISTS(SELECT 1 FROM course_participation p WHERE p.course_id=c.id AND p.user_id=#{user})</if>
 """;
 @Select("<script>"+COLUMNS+FILTER+" ORDER BY <choose><when test=\"sort == 'rating'\">rating DESC,</when><when test=\"sort == 'reviews'\">reviewCount DESC,</when></choose>c.code LIMIT #{size} OFFSET #{offset}</script>")
 List<Map<String,Object>> list(@Param("code")String code,@Param("name")String name,@Param("keyword")String keyword,@Param("level")String level,@Param("subject")String subject,@Param("mine")boolean mine,@Param("user")String user,@Param("sort")String sort,@Param("size")int size,@Param("offset")int offset);
 @Select("<script>SELECT COUNT(*) FROM course c "+FILTER+"</script>") long count(@Param("code")String code,@Param("name")String name,@Param("keyword")String keyword,@Param("level")String level,@Param("subject")String subject,@Param("mine")boolean mine,@Param("user")String user);
 @Select(COLUMNS+" WHERE c.id=#{id} AND c.status=1") Map<String,Object> detail(@Param("id")long id,@Param("user")String user);
 @Select("SELECT subject,COUNT(*) total FROM course WHERE status=1 AND (#{level}='' OR level=#{level}) GROUP BY subject ORDER BY subject") List<Map<String,Object>> subjects(String level);
 @Insert("INSERT IGNORE INTO course_participation(course_id,user_id) VALUES(#{id},#{user})") void join(@Param("id")long id,@Param("user")String user);
 @Delete("DELETE FROM course_participation WHERE course_id=#{id} AND user_id=#{user}") void leave(@Param("id")long id,@Param("user")String user);
 @Select("SELECT COUNT(*) FROM course_participation WHERE course_id=#{id} AND user_id=#{user}") int joined(@Param("id")long id,@Param("user")String user);
 @Select("SELECT CAST(r.id AS CHAR) id,r.semester,r.overall,r.difficulty,r.workload,r.teaching,r.exam_difficulty examDifficulty,r.midterm,r.final_exam finalExam,r.open_book openBook,r.content,r.anonymous,r.created_at createdAt,IF(r.anonymous,'匿名用户',u.nickname) authorName,IF(r.anonymous,NULL,u.id) authorId FROM course_review r LEFT JOIN user u ON u.id=r.user_id WHERE r.course_id=#{id} AND r.status=1 ORDER BY r.created_at DESC") List<Map<String,Object>> reviews(long id);
 @Insert("INSERT INTO course_review(id,course_id,user_id,semester,overall,difficulty,workload,teaching,exam_difficulty,midterm,final_exam,open_book,content,anonymous) VALUES(#{id},#{courseId},#{user},#{r.semester},#{r.overall},#{r.difficulty},#{r.workload},#{r.teaching},#{r.examDifficulty},#{r.midterm},#{r.finalExam},#{r.openBook},#{r.content},#{r.anonymous})") void review(@Param("id")long id,@Param("courseId")long courseId,@Param("user")String user,@Param("r")Map<String,Object> review);
 @Insert("INSERT INTO course_vote(course_id,user_id,recommend) VALUES(#{id},#{user},#{recommend}) ON DUPLICATE KEY UPDATE recommend=#{recommend}") void vote(@Param("id")long id,@Param("user")String user,@Param("recommend")boolean recommend);
 @Insert("INSERT IGNORE INTO course_bookmark(course_id,user_id) VALUES(#{id},#{user})") void bookmark(@Param("id")long id,@Param("user")String user);
 @Delete("DELETE FROM course_bookmark WHERE course_id=#{id} AND user_id=#{user}") void unbookmark(@Param("id")long id,@Param("user")String user);
 @Select("SELECT CAST(g.id AS CHAR) id,g.name,g.description,g.capacity,g.skills,g.owner_id=#{user} own,(SELECT COUNT(*) FROM course_group_member m WHERE m.group_id=g.id) memberCount,EXISTS(SELECT 1 FROM course_group_member m WHERE m.group_id=g.id AND m.user_id=#{user}) joined FROM course_group g WHERE g.course_id=#{id} AND g.status=1") List<Map<String,Object>> groups(@Param("id")long id,@Param("user")String user);
 @Update("UPDATE course_group SET skills=#{skills},contact_wechat=#{wechat},contact_note=#{note} WHERE id=#{id}") void groupMetadata(@Param("id")long id,@Param("skills")String skills,@Param("wechat")String wechat,@Param("note")String note);
 @Select("SELECT m.user_id userId,u.nickname,u.avatar,m.user_id=g.owner_id owner FROM course_group_member m JOIN course_group g ON g.id=m.group_id JOIN user u ON u.id=m.user_id WHERE m.group_id=#{id} ORDER BY (m.user_id=g.owner_id) DESC,m.user_id") List<Map<String,Object>> memberList(long id);
 @Update("UPDATE course_group SET name=#{name},description=#{description},capacity=#{capacity} WHERE id=#{id}") void updateGroup(@Param("id")long id,@Param("name")String name,@Param("description")String description,@Param("capacity")int capacity);
 @Update("UPDATE course_group SET status=2 WHERE id=#{id}") void closeGroup(long id);
 @Insert("INSERT INTO course_group(id,course_id,owner_id,name,description,capacity) VALUES(#{id},#{courseId},#{user},#{name},#{description},#{capacity})") void group(@Param("id")long id,@Param("courseId")long courseId,@Param("user")String user,@Param("name")String name,@Param("description")String description,@Param("capacity")int capacity);
 @Select("SELECT * FROM course_group WHERE id=#{id} AND status=1 FOR UPDATE") Map<String,Object> lockGroup(long id);
 @Select("SELECT COUNT(*) FROM course_group_member WHERE group_id=#{id}") int members(long id);
 @Insert("INSERT IGNORE INTO course_group_member(group_id,user_id) VALUES(#{id},#{user})") void joinGroup(@Param("id")long id,@Param("user")String user);
 @Delete("DELETE FROM course_group_member WHERE group_id=#{id} AND user_id=#{user}") void leaveGroup(@Param("id")long id,@Param("user")String user);
 @Select("SELECT CAST(id AS CHAR) id,title,category,url,created_at createdAt FROM course_material WHERE course_id=#{id} AND status=1 ORDER BY created_at DESC") List<Map<String,Object>> materials(long id);
 @Insert("INSERT INTO course_material(id,course_id,user_id,title,category,url) VALUES(#{id},#{courseId},#{user},#{title},#{category},#{url})") void material(@Param("id")long id,@Param("courseId")long courseId,@Param("user")String user,@Param("title")String title,@Param("category")String category,@Param("url")String url);
 @Insert("INSERT INTO course(id,code,name,subtitle,level,subject,credits,description) VALUES(#{id},#{r.code},#{r.name},#{r.subtitle},#{r.level},#{r.subject},#{r.credits},#{r.description})") void create(@Param("id")long id,@Param("r")Map<String,Object> course);
}
