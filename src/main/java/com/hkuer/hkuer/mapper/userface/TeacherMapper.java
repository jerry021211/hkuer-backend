package com.hkuer.hkuer.mapper.userface;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.hkuer.hkuer.entity.PO.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    @Select("SELECT * FROM teacher WHERE id = #{teacherId} FOR UPDATE")
    Teacher selectByIdForUpdate(@Param("teacherId") String teacherId);

    @Update("""
            UPDATE teacher t
            LEFT JOIN (
                SELECT target_id, SUM(score) AS total_score, COUNT(*) AS rating_count
                FROM ratings
                GROUP BY target_id
            ) ratings_summary ON ratings_summary.target_id = t.id
            SET t.total_score = COALESCE(ratings_summary.total_score, 0),
                t.rating_count = COALESCE(ratings_summary.rating_count, 0),
                t.average_rating = CASE
                    WHEN COALESCE(ratings_summary.rating_count, 0) = 0 THEN 0
                    ELSE ROUND(ratings_summary.total_score / ratings_summary.rating_count, 2)
                END
            WHERE COALESCE(t.total_score, 0) <> COALESCE(ratings_summary.total_score, 0)
               OR COALESCE(t.rating_count, 0) <> COALESCE(ratings_summary.rating_count, 0)
               OR COALESCE(t.average_rating, 0) <> CASE
                    WHEN COALESCE(ratings_summary.rating_count, 0) = 0 THEN 0
                    ELSE ROUND(ratings_summary.total_score / ratings_summary.rating_count, 2)
                END
            """)
    int reconcileRatingStats();

    /**
     * 自定义查询：获取教师列表，并包含当前用户的评分（如果已登录）
     * * @param page    分页参数（MyBatis-Plus 会自动处理 LIMIT）
     * @param wrapper 查询条件（对应 XML 中的 ${ew.customSqlSegment}，包含 WHERE 和 ORDER BY）
     * @param userId  当前用户ID（对应 XML 中的 #{userId}）
     * @return 分页结果
     */
    IPage<Teacher> selectTeacherListWithUserRating(IPage<Teacher> page,
                                                   @Param(Constants.WRAPPER) Wrapper<Teacher> wrapper,
                                                   @Param("userId") String userId);
}
