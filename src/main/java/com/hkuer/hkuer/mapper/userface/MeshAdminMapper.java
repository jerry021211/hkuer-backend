package com.hkuer.hkuer.mapper.userface;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface MeshAdminMapper {
 class Sql {
  private static final Map<String,String> TABLES=Map.of("post","post","answer","answer","review","course_review","material","course_material","group","course_group","course","course","announcement","announcement");
  private static String table(Map<String,Object> p){String name=TABLES.get(p.get("kind"));if(name==null)throw new IllegalArgumentException("无效内容类型");return name;}
  public static String list(Map<String,Object> p){String table=table(p);String title=switch(table){case "post","course_material","announcement"->"title";case "course_group","course"->"name";default->"content";};return "SELECT CAST(id AS CHAR) id,"+title+" title,status FROM "+table+" ORDER BY id DESC LIMIT #{size} OFFSET #{offset}";}
  public static String count(Map<String,Object> p){return "SELECT COUNT(*) FROM "+table(p);}
  public static String status(Map<String,Object> p){return "SELECT status FROM "+table(p)+" WHERE id=#{id} FOR UPDATE";}
  public static String update(Map<String,Object> p){return "UPDATE "+table(p)+" SET status=#{status} WHERE id=#{id}";}
 }
 @SelectProvider(type=Sql.class,method="list") List<Map<String,Object>> list(@Param("kind")String kind,@Param("size")int size,@Param("offset")int offset);
 @SelectProvider(type=Sql.class,method="count") long count(@Param("kind")String kind);
 @SelectProvider(type=Sql.class,method="status") Integer status(@Param("kind")String kind,@Param("id")long id);
 @UpdateProvider(type=Sql.class,method="update") int update(@Param("kind")String kind,@Param("id")long id,@Param("status")int status);
 @Insert("INSERT INTO moderation_audit(id,actor_id,target_type,target_id,previous_status,new_status,reason) VALUES(#{auditId},#{actor},#{kind},#{id},#{before},#{status},#{reason})") void audit(@Param("auditId")long auditId,@Param("actor")String actor,@Param("kind")String kind,@Param("id")long id,@Param("before")int before,@Param("status")int status,@Param("reason")String reason);
 @Insert("INSERT INTO announcement(id,title,content,pinned,category) VALUES(#{id},#{title},#{content},#{pinned},#{category})") void announcement(@Param("id")long id,@Param("title")String title,@Param("content")String content,@Param("pinned")boolean pinned,@Param("category")String category);
}
