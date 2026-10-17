package com.hkuer.hkuer.mapper.userface;

import org.apache.ibatis.annotations.*;
import java.util.*;

public interface MeshContentMapper {
    @Insert("INSERT IGNORE INTO publish_batch(user_id,request_key,payload_hash) VALUES(#{user},#{key},#{hash})") void beginBatch(@Param("user")String user,@Param("key")String key,@Param("hash")String hash);
    @Select("SELECT payload_hash hash,result_json result FROM publish_batch WHERE user_id=#{user} AND request_key=#{key} FOR UPDATE") Map<String,Object> lockBatch(@Param("user")String user,@Param("key")String key);
    @Update("UPDATE publish_batch SET result_json=#{result} WHERE user_id=#{user} AND request_key=#{key}") void finishBatch(@Param("user")String user,@Param("key")String key,@Param("result")String result);

    String FILTER="""
        FROM post p JOIN post_meta m ON m.post_id=p.id LEFT JOIN post_content c ON c.post_id=p.id
        LEFT JOIN user u ON u.id=p.author_id WHERE p.status=1
        <if test="kind != null and kind != ''">AND m.kind=#{kind}</if>
        <if test="keyword != null and keyword != ''">AND (p.title LIKE CONCAT('%',#{keyword},'%') OR c.content_md LIKE CONCAT('%',#{keyword},'%') OR m.company LIKE CONCAT('%',#{keyword},'%') OR m.role LIKE CONCAT('%',#{keyword},'%') OR m.region LIKE CONCAT('%',#{keyword},'%') OR EXISTS(SELECT 1 FROM market_item mi WHERE mi.post_id=p.id AND (mi.brand LIKE CONCAT('%',#{keyword},'%') OR mi.trade_location LIKE CONCAT('%',#{keyword},'%'))))</if>
        <if test="tag != null and tag != ''">AND JSON_CONTAINS(m.tags,JSON_QUOTE(#{tag}))</if>
        <if test="category != null and category != ''">AND m.category=#{category}</if>
        <if test="region != null and region != ''">
          AND <choose>
            <when test="region == '港岛'">m.region IN ('港岛','薄扶林','坚尼地城','西营盘','石塘咀','上环','中环','金钟','湾仔','铜锣湾','北角','鲗鱼涌','香港仔')</when>
            <when test="region == '九龙'">m.region IN ('九龙','观塘','尖沙咀','旺角','油麻地','红磡','何文田','九龙塘','深水埗')</when>
            <when test="region == '新界'">m.region IN ('新界','荃湾','沙田','大埔','屯门','元朗','将军澳')</when>
            <when test="region == '深圳'">m.region IN ('深圳','深圳南山','深圳福田','深圳罗湖')</when>
            <otherwise>m.region=#{region}</otherwise>
          </choose>
        </if>
        <if test="courseId != null">AND m.course_id=#{courseId}</if>
        <if test="author != null and author != ''">AND p.author_id=#{author} AND m.anonymous=false</if>
        """;
    String COLUMNS="""
        SELECT CAST(p.id AS CHAR) id,p.title,c.content_md content,p.status,p.created_at createdAt,p.author_id authorId,
        u.nickname authorName,u.avatar authorAvatar,p.like_num likeCount,p.fav_num favoriteCount,p.view_count viewCount,
        m.kind,m.category,m.anonymous,m.images,m.tags,m.region,m.rating,m.company,m.role,CAST(m.course_id AS CHAR) courseId,
        (SELECT price FROM market_item i WHERE i.post_id=p.id) price,
        COALESCE((SELECT negotiable FROM market_item i WHERE i.post_id=p.id),(SELECT negotiable FROM rental_post i WHERE i.post_id=p.id),false) negotiable,
        (SELECT condition_label FROM market_item i WHERE i.post_id=p.id) conditionLabel,
        (SELECT approximate FROM rental_post i WHERE i.post_id=p.id) approximate,
        (SELECT gender FROM rental_post i WHERE i.post_id=p.id) gender,
        (SELECT trade_location FROM market_item i WHERE i.post_id=p.id) tradeLocation,
        (SELECT brand FROM market_item i WHERE i.post_id=p.id) brand,
        (SELECT sale_status FROM market_item i WHERE i.post_id=p.id) saleStatus,
        (SELECT rental_type FROM rental_post i WHERE i.post_id=p.id) rentalType,
        (SELECT rent FROM rental_post i WHERE i.post_id=p.id) rent,
        (SELECT budget_min FROM rental_post i WHERE i.post_id=p.id) budgetMin,
        (SELECT budget_max FROM rental_post i WHERE i.post_id=p.id) budgetMax,
        (SELECT move_in FROM rental_post i WHERE i.post_id=p.id) moveIn,
        (SELECT lease_term FROM rental_post i WHERE i.post_id=p.id) leaseTerm,
        (SELECT room_types FROM rental_post i WHERE i.post_id=p.id) roomTypes,
        (SELECT habits FROM rental_post i WHERE i.post_id=p.id) habits,
        (SELECT listing_status FROM rental_post i WHERE i.post_id=p.id) listingStatus,
        (SELECT COUNT(*) FROM answer a WHERE a.question_id=p.id AND a.status=1) answerCount,
        (SELECT COUNT(*) FROM comment_article ca WHERE ca.post_id=p.id AND ca.status=1) commentCount,
        (SELECT COUNT(*) FROM question_interaction qi WHERE qi.question_id=p.id AND qi.kind='same') sameCount,
        EXISTS(SELECT 1 FROM post_like pl WHERE pl.target_id=p.id AND pl.user_id=#{viewer}) liked,
        EXISTS(SELECT 1 FROM post_favorite pf WHERE pf.target_id=p.id AND pf.user_id=#{viewer}) favorited,
        EXISTS(SELECT 1 FROM question_interaction qi WHERE qi.question_id=p.id AND qi.user_id=#{viewer} AND qi.kind='same') same,
        EXISTS(SELECT 1 FROM question_interaction qi WHERE qi.question_id=p.id AND qi.user_id=#{viewer} AND qi.kind='watch') watching,
        EXISTS(SELECT 1 FROM question_interaction qi WHERE qi.question_id=p.id AND qi.user_id=#{viewer} AND qi.kind='dislike') disliked
        """;
    @Select("<script>"+COLUMNS+FILTER+" ORDER BY <choose><when test=\"sort == 'low'\">price IS NULL,price ASC,</when><when test=\"sort == 'high'\">price DESC,</when></choose>p.is_top DESC,p.created_at DESC,p.id DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String,Object>> feed(@Param("kind") String kind,@Param("keyword") String keyword,@Param("tag") String tag,
        @Param("category") String category,@Param("author") String author,@Param("region") String region,@Param("courseId") Long courseId,@Param("viewer") String viewer,@Param("sort") String sort,@Param("size") int size,@Param("offset") int offset);
    @Select("<script>SELECT COUNT(*) "+FILTER+"</script>")
    long count(@Param("kind") String kind,@Param("keyword") String keyword,@Param("tag") String tag,@Param("category") String category,@Param("author") String author,@Param("region") String region,@Param("courseId") Long courseId);
    @Select(COLUMNS+" FROM post p JOIN post_meta m ON m.post_id=p.id LEFT JOIN post_content c ON c.post_id=p.id LEFT JOIN user u ON u.id=p.author_id WHERE p.id=#{id} AND (p.status=1 OR p.author_id=#{viewer} OR #{admin}=true)")
    Map<String,Object> detail(@Param("id") long id,@Param("viewer") String viewer,@Param("admin") boolean admin);
    @Insert("INSERT INTO post_meta(post_id,kind,category,anonymous,images,tags,region,rating,company,role,course_id) VALUES(#{id},#{r.kind},#{r.category},#{r.anonymous},#{images},#{tags},#{r.region},#{r.rating},#{r.company},#{r.role},#{r.courseId})")
    void insertMeta(@Param("id") long id,@Param("r") com.hkuer.hkuer.entity.DTO.MeshContentRequest request,@Param("images") String images,@Param("tags") String tags);
    @Update("UPDATE post_meta SET category=#{r.category},anonymous=#{r.anonymous},images=#{images},tags=#{tags},region=#{r.region},rating=#{r.rating},company=#{r.company},role=#{r.role},course_id=#{r.courseId} WHERE post_id=#{id}")
    void updateMeta(@Param("id")long id,@Param("r")com.hkuer.hkuer.entity.DTO.MeshContentRequest request,@Param("images")String images,@Param("tags")String tags);
    @Update("UPDATE market_item SET price=#{r.price},negotiable=#{r.negotiable},brand=#{r.brand},trade_location=#{r.tradeLocation},condition_label=#{r.conditionLabel} WHERE post_id=#{id}")
    void updateMarket(@Param("id")long id,@Param("r")com.hkuer.hkuer.entity.DTO.MeshContentRequest request);
    @Update("UPDATE rental_post SET rental_type=#{r.rentalType},rent=#{r.rent},negotiable=#{r.negotiable},approximate=#{r.approximate},budget_min=#{r.budgetMin},budget_max=#{r.budgetMax},room_types=#{rooms},lease_term=#{r.leaseTerm},move_in=#{r.moveIn},gender=#{r.gender},habits=#{habits} WHERE post_id=#{id}")
    void updateRental(@Param("id")long id,@Param("r")com.hkuer.hkuer.entity.DTO.MeshContentRequest request,@Param("rooms")String rooms,@Param("habits")String habits);
    @Select("SELECT CAST(a.id AS CHAR) id,CAST(a.question_id AS CHAR) questionId,a.author_id authorId,u.nickname authorName,u.avatar authorAvatar,a.content,a.anonymous,a.accepted,a.created_at createdAt,(SELECT COUNT(*) FROM answer_like l WHERE l.answer_id=a.id) likeCount,EXISTS(SELECT 1 FROM answer_like l WHERE l.answer_id=a.id AND l.user_id=#{viewer}) liked FROM answer a LEFT JOIN user u ON u.id=a.author_id WHERE a.question_id=#{id} AND a.status=1 ORDER BY a.accepted DESC,a.created_at,a.id")
    List<Map<String,Object>> answers(@Param("id") long id,@Param("viewer") String viewer);
    @Insert("INSERT INTO answer(id,question_id,author_id,content,anonymous) VALUES(#{id},#{questionId},#{author},#{content},#{anonymous})")
    void answer(@Param("id") long id,@Param("questionId") long questionId,@Param("author") String author,@Param("content") String content,@Param("anonymous") boolean anonymous);
    @Insert("INSERT IGNORE INTO question_interaction(question_id,user_id,kind) VALUES(#{id},#{user},#{kind})")
    void follow(@Param("id") long id,@Param("user") String user,@Param("kind") String kind);
    @Delete("DELETE FROM question_interaction WHERE question_id=#{id} AND user_id=#{user} AND kind=#{kind}")
    void unfollow(@Param("id") long id,@Param("user") String user,@Param("kind") String kind);
    @Select("SELECT CAST(id AS CHAR) id,title,content,pinned,category,created_at createdAt FROM announcement WHERE status=1 ORDER BY pinned DESC,created_at DESC,id DESC")
    List<Map<String,Object>> announcements();
    @Select("SELECT (SELECT COUNT(*) FROM page_feedback WHERE page_key=#{page}) count,EXISTS(SELECT 1 FROM page_feedback WHERE page_key=#{page} AND user_id=#{user}) helpful")
    Map<String,Object> pageFeedback(@Param("page")String page,@Param("user")String user);
    @Insert("INSERT IGNORE INTO page_feedback(page_key,user_id) VALUES(#{page},#{user})")
    void addPageFeedback(@Param("page")String page,@Param("user")String user);
    @Delete("DELETE FROM page_feedback WHERE page_key=#{page} AND user_id=#{user}")
    void removePageFeedback(@Param("page")String page,@Param("user")String user);
    @Select("SELECT COUNT(*) FROM answer a JOIN post p ON p.id=a.question_id WHERE p.status=1 AND a.status=1") long answerCount();
    @Select("SELECT CAST(id AS CHAR) id,author_id authorId,CAST(question_id AS CHAR) questionId,status FROM answer WHERE id=#{id}") Map<String,Object> answerById(long id);
    @Insert("INSERT IGNORE INTO answer_like(answer_id,user_id) VALUES(#{id},#{user})") void likeAnswer(@Param("id") long id,@Param("user") String user);
    @Delete("DELETE FROM answer_like WHERE answer_id=#{id} AND user_id=#{user}") void unlikeAnswer(@Param("id") long id,@Param("user") String user);
    @Update("UPDATE answer SET status=2 WHERE id=#{id}") void deleteAnswer(long id);
    @Select("SELECT author_id authorId,question_id questionId,status FROM answer WHERE id=#{id} FOR UPDATE") Map<String,Object> lockAnswer(long id);
    @Update("UPDATE answer SET content=#{content},anonymous=#{anonymous} WHERE id=#{id}") void updateAnswer(@Param("id")long id,@Param("content")String content,@Param("anonymous")boolean anonymous);
    @Update("UPDATE answer SET accepted=(id=#{answerId}) WHERE question_id=#{id}") void acceptAnswer(@Param("id") long id,@Param("answerId") long answerId);
    @Insert("INSERT INTO notification(id,user_id,actor_id,kind,title,content,target_path) VALUES(#{id},#{recipient},#{actor},#{kind},#{title},#{content},#{path})")
    void notify(@Param("id") long id,@Param("recipient") String recipient,@Param("actor") String actor,@Param("kind") String kind,@Param("title") String title,@Param("content") String content,@Param("path") String path);
    @Insert("INSERT INTO market_item(post_id,price,negotiable,brand,trade_location,condition_label) VALUES(#{id},#{r.price},#{r.negotiable},#{r.brand},#{r.tradeLocation},#{r.conditionLabel})")
    void market(@Param("id")long id,@Param("r")com.hkuer.hkuer.entity.DTO.MeshContentRequest request);
    @Insert("INSERT INTO rental_post(post_id,rental_type,rent,negotiable,approximate,budget_min,budget_max,room_types,lease_term,move_in,gender,habits) VALUES(#{id},#{r.rentalType},#{r.rent},#{r.negotiable},#{r.approximate},#{r.budgetMin},#{r.budgetMax},#{rooms},#{r.leaseTerm},#{r.moveIn},#{r.gender},#{habits})")
    void rental(@Param("id")long id,@Param("r")com.hkuer.hkuer.entity.DTO.MeshContentRequest request,@Param("rooms")String rooms,@Param("habits")String habits);
    @Update("UPDATE market_item SET sale_status=#{status} WHERE post_id=#{id}") void marketStatus(@Param("id")long id,@Param("status")String status);
    @Update("UPDATE rental_post SET listing_status=#{status} WHERE post_id=#{id}") void rentalStatus(@Param("id")long id,@Param("status")String status);
    @Select("SELECT COUNT(*) FROM post_meta WHERE post_id=#{id} AND anonymous=true") int anonymous(long id);
}
