package com.hkuer.hkuer.annotation;

import java.lang.annotation.*;

/**
 * 浏览量统计注解
 * 标注在需要统计浏览量的接口方法上
 * 
 * 使用方式：
 * @ViewCount(postIdParam = "postId")
 * public Result getPost(@RequestParam Long postId) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ViewCount {
    
    /**
     * 帖子ID参数名
     * 用于从方法参数中获取帖子ID
     */
    String postIdParam() default "postId";
}
