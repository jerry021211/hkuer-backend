package com.hkuer.hkuer.mapper.userface;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface MeshAccountMapper {
 @Select("SELECT last_rank FROM badge_registration_sequence WHERE id=1 FOR UPDATE") long lockRegistrationSequence();
 @Insert("INSERT INTO badge_registration(user_id,registration_rank) VALUES(#{user},#{rank})") void registerRank(@Param("user")String user,@Param("rank")long rank);
 @Update("UPDATE badge_registration_sequence SET last_rank=#{rank} WHERE id=1") void advanceRegistrationSequence(long rank);
 @Select("SELECT b.code,b.name,b.description,b.symbol FROM user_badge ub JOIN badge b ON b.code=ub.badge_code WHERE ub.user_id=#{user}") List<Map<String,Object>> badges(String user);
 @Insert("INSERT IGNORE INTO user_badge(user_id,badge_code) VALUES(#{user},#{code})") void award(@Param("user")String user,@Param("code")String code);
 @Select("SELECT COUNT(*) FROM user_badge WHERE user_id=#{user} AND badge_code=#{code}") int owns(@Param("user")String user,@Param("code")String code);
 @Insert("INSERT INTO user_profile(user_id,active_badge) VALUES(#{user},#{code}) ON DUPLICATE KEY UPDATE active_badge=#{code}") void wear(@Param("user")String user,@Param("code")String code);
 @Insert("INSERT INTO user_profile(user_id,hku_email,verified_hku) VALUES(#{user},#{email},true) ON DUPLICATE KEY UPDATE hku_email=#{email},verified_hku=true") void verify(@Param("user")String user,@Param("email")String email);
}
