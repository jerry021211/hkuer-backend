package com.hkuer.hkuer.mapper.userface;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkuer.hkuer.entity.PO.Favorite;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Insert("""
            INSERT IGNORE INTO post_favorite (favorite_id, user_id, target_id, created_at)
            VALUES (#{favoriteId}, #{userId}, #{targetId}, #{createdAt})
            """)
    int insertIgnore(Favorite favorite);
}

