package com.hkuer.hkuer.annotation;

import java.lang.annotation.*;

/**
 * 禁言检查注解
 * 标注在需要检查用户禁言状态的 Controller 方法上（如发帖、评论接口）
 * 
 * 使用方式：
 * @CheckMute
 * public Result createPost(@RequestBody PostInsertRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckMute {
    
    /**
     * 禁言提示消息前缀（可自定义）
     */
    String message() default "您已被禁言";
}
