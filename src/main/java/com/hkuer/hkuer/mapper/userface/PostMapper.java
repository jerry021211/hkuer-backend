package com.hkuer.hkuer.mapper.userface;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkuer.hkuer.entity.PO.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT * FROM post WHERE id = #{postId} FOR UPDATE")
    Post selectByIdForUpdate(@Param("postId") Long postId);

    @Update("""
            UPDATE post
            SET like_num = GREATEST(COALESCE(like_num, 0) + #{delta}, 0)
            WHERE id = #{postId}
            """)
    int incrementLikeCount(@Param("postId") Long postId, @Param("delta") long delta);

    @Update("""
            UPDATE post
            SET fav_num = GREATEST(COALESCE(fav_num, 0) + #{delta}, 0)
            WHERE id = #{postId}
            """)
    int incrementFavoriteCount(@Param("postId") Long postId, @Param("delta") long delta);

    @Update("""
            UPDATE post
            SET view_count = COALESCE(view_count, 0) + #{delta}
            WHERE id = #{postId}
            """)
    int incrementViewCount(@Param("postId") Long postId, @Param("delta") long delta);

    @Update("""
            UPDATE post p
            LEFT JOIN (
                SELECT target_id, COUNT(*) AS count_value
                FROM post_like
                GROUP BY target_id
            ) likes ON likes.target_id = p.id
            LEFT JOIN (
                SELECT target_id, COUNT(*) AS count_value
                FROM post_favorite
                GROUP BY target_id
            ) favorites ON favorites.target_id = p.id
            SET p.like_num = COALESCE(likes.count_value, 0),
                p.fav_num = COALESCE(favorites.count_value, 0)
            WHERE COALESCE(p.like_num, 0) <> COALESCE(likes.count_value, 0)
               OR COALESCE(p.fav_num, 0) <> COALESCE(favorites.count_value, 0)
            """)
    int reconcileInteractionCounts();
}
