package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.PO.Teacher;
public interface RatingService {
    /**
     * 用户打分（高并发入口）
     */
    Result rateTeacher(String teacherId, Integer score);

    /**
     * 获取教师实时评分（优先读 Redis）
     */
    Teacher getTeacherStats(String teacherId);

    Result getTeacherList(int current, int size, String keyword, Boolean isDesc);

}
