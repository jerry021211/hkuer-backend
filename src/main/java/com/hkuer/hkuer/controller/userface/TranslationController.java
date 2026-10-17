package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.userface.AliyunTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/translation")
public class TranslationController {

    @Autowired
    private AliyunTranslationService aliyunTranslationService;

    /**
     * 文本翻译接口
     * 请求体: { "text": "你好", "targetLang": "en" }
     */
    @PostMapping("/text")
    public Result translate(@RequestBody Map<String, String> params) {
        String text = params.get("text");
        String targetLang = params.get("targetLang");
        return aliyunTranslationService.translate(text, targetLang);
    }
}
