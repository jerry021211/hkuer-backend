package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.TeacherCommentInsertRequest;
import com.hkuer.hkuer.service.userface.TeacherCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户评论老师
 */
@RestController
@RequestMapping("/course-comment")
public class TeacherCommentController {

    @Autowired
    private TeacherCommentService teacherCommentService;

    /**
     * 发表评论（需登录，userId 由后端从上下文获取）
     */
    @PostMapping("/add")
    public Result add(@RequestBody TeacherCommentInsertRequest request) {
        try {
            return teacherCommentService.insert(request);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", e.getMessage());
        }
    }

    /**
     * 按教师 ID 分页查询评论，按创建时间倒序
     */
    @GetMapping("/page")
    public Result page(@RequestParam String teacherId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        return teacherCommentService.pageByTeacherId(teacherId, page, size);
    }
}
