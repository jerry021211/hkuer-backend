package com.hkuer.hkuer.service.userface;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.TeacherCommentInsertRequest;
import com.hkuer.hkuer.entity.PO.TeacherComment;

/**
 * 用户评论老师
 */
public interface TeacherCommentService {

    /**
     * 插入评论（userId 从当前登录用户获取）
     */
    Result insert(TeacherCommentInsertRequest request);

    /**
     * 按教师 ID 分页查询评论，按创建时间倒序
     */
    Result pageByTeacherId(String teacherId, Integer current, Integer size);
}
