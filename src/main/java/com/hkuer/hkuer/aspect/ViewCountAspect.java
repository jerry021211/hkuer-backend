package com.hkuer.hkuer.aspect;

import com.hkuer.hkuer.annotation.ViewCount;
import com.hkuer.hkuer.service.userface.ViewCountService;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 浏览量统计 AOP 切面
 * 
 * 特点：
 * 1. 只在方法执行成功后统计（@AfterReturning）
 * 2. 统计逻辑通过 @Async 异步执行，不阻塞主线程
 * 3. 支持用户去重（同一用户24小时内多次访问只计1次）
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@Order(1)  // 设置切面优先级，数字越小优先级越高
public class ViewCountAspect {

    private final ViewCountService viewCountService;

    /**
     * 在标注了 @ViewCount 的方法成功返回后执行
     * 统计逻辑是异步的，不会阻塞主线程
     */
    @AfterReturning(pointcut = "@annotation(viewCount)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, ViewCount viewCount, Object result) {
        try {
            // 从方法参数中获取 postId
            Long postId = extractPostId(joinPoint, viewCount.postIdParam());
            
            if (postId == null) {
                log.warn("无法从方法参数中获取 postId，参数名: {}", viewCount.postIdParam());
                return;
            }
            
            // 获取当前用户ID（用于去重）
            String userId = UserContext.getCurrentUserId();
            
            // 异步增加浏览量（方法内部使用 @Async，不阻塞当前线程）
            viewCountService.incrementViewCountAsync(postId, userId);
            
            log.debug("触发浏览量统计，postId: {}, userId: {}", postId, userId);
            
        } catch (Exception e) {
            // 统计异常不应影响主业务
            log.error("浏览量统计切面执行异常", e);
        }
    }

    /**
     * 从方法参数中提取 postId
     */
    private Long extractPostId(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        // 遍历参数查找匹配的参数名
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                Object arg = args[i];
                if (arg instanceof Long) {
                    return (Long) arg;
                } else if (arg instanceof Integer) {
                    return ((Integer) arg).longValue();
                } else if (arg instanceof String) {
                    try {
                        return Long.parseLong((String) arg);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        
        // 如果参数名匹配失败，尝试直接查找名为 postId 的参数
        String[] parameterNames = signature.getParameterNames();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(paramName)) {
                    Object arg = args[i];
                    if (arg instanceof Long) {
                        return (Long) arg;
                    } else if (arg instanceof Integer) {
                        return ((Integer) arg).longValue();
                    } else if (arg instanceof String) {
                        try {
                            return Long.parseLong((String) arg);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                }
            }
        }
        
        return null;
    }
}
