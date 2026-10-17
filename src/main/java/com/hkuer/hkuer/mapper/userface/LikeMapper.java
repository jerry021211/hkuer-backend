package com.hkuer.hkuer.mapper.userface;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hkuer.hkuer.entity.PO.Like;
import com.hkuer.hkuer.entity.VO.LikedPostResponse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    @Insert("""
            INSERT IGNORE INTO post_like (like_id, user_id, target_id, created_at)
            VALUES (#{likeId}, #{userId}, #{targetId}, #{createdAt})
            """)
    int insertIgnore(Like like);

    @Select("""
            <script>
            SELECT p.id AS postId,
                   p.title,
                   pc.content_md AS content
            FROM post_like pl
            INNER JOIN post p ON p.id = pl.target_id
            LEFT JOIN post_content pc ON pc.post_id = p.id
            WHERE pl.user_id = #{userId}
              AND p.status = 1
            <if test="keyword != null and keyword != ''">
              AND pc.content_md LIKE CONCAT('%', #{keyword}, '%')
            </if>
            ORDER BY p.published_at DESC
            </script>
            """)
    IPage<LikedPostResponse> selectLikedPostPage(Page<LikedPostResponse> page,
                                                 @Param("userId") String userId,
                                                 @Param("keyword") String keyword);
}


