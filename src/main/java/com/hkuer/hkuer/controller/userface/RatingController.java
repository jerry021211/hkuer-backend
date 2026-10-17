package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.PO.Teacher;
import com.hkuer.hkuer.service.userface.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rating")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    /**
     * 提交评分
     * 返回 Result，前端通过 result.code 判断是否成功
     */
    @PostMapping("/add")
    public Result addRating(@RequestParam String teacherId,
                            @RequestParam Integer score) {

        // 1. 简单的参数校验
        if (score < 1 || score > 5) {
            return Result.error("400", "分数必须在 1-5 之间");
        }
        try {
            return ratingService.rateTeacher(teacherId, score);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", e.getMessage());
        }
    }

    /**
     * 获取评分详情
     */
    @GetMapping("/info")
    public Result getRatingInfo(@RequestParam String teacherId) {
        // 1. 调用 Service 获取 Teacher 对象
        Teacher teacher = ratingService.getTeacherStats(teacherId);

        if (teacher != null) {
            return Result.success(teacher);
        } else {
            return Result.success(null);
        }
    }

    @GetMapping("/list")
    public Result getTeacherList(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "true") Boolean isDesc) {
        return ratingService.getTeacherList(page, size, keyword, isDesc);
    }
}
