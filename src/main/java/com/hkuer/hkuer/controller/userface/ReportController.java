package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.ReportInsertRequest;
import com.hkuer.hkuer.service.userface.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 举报接口控制器（用户端）
 * 普通用户可访问，无需管理员权限
 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 提交举报
     *
     * @param request 举报请求
     */
    @PostMapping("/submit")
    public Result submitReport(@RequestBody ReportInsertRequest request) {
        return reportService.submitReport(request);
    }
}
