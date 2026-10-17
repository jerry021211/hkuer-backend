package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;

public interface AliyunTranslationService {

    public Result translate(String sourceText, String targetLang);
}
