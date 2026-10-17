package com.hkuer.hkuer.mapper.userface;
import org.apache.ibatis.annotations.*;
public interface MeshReportMapper {
    @Select("SELECT author_id FROM answer WHERE id=#{id} AND status=1") String answer(long id);
    @Select("SELECT user_id FROM course_review WHERE id=#{id} AND status=1") String review(long id);
    @Select("SELECT user_id FROM course_material WHERE id=#{id} AND status=1") String material(long id);
}
