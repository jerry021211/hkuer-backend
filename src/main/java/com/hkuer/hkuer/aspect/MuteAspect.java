package com.hkuer.hkuer.aspect;

import com.hkuer.hkuer.annotation.CheckMute;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.service.userface.MuteService;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 禁言检查 AOP 切面
 * 
 * 特点：
 * 1. 使用 @Around 环绕通知，在方法执行前进行拦截
 * 2. 直接查询 Redis 判断禁言状态，高性能无数据库查询
 * 3. 被禁言时抛出 BusinessException，由全局异常处理器统一处理
 * 
 * 使用方式：
 * 在 Controller 方法上添加 @CheckMute 注解即可
 * 
 * @CheckMute
 * public Result createPost(...) { ... }
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@Order(0)  // 优先级最高，在其他切面之前执行
public class MuteAspect {

    private final MuteService muteService;

    /**
     * 环绕通知：拦截标注了 @CheckMute 的方法
     * 在方法执行前检查用户是否被禁言
     */
    @Around("@annotation(checkMute)")
    public Object checkMuteStatus(ProceedingJoinPoint joinPoint, CheckMute checkMute) throws Throwable {
        // 1. 获取当前用户 ID
        String userId = UserContext.getCurrentUserId();
        
        if (userId == null || userId.trim().isEmpty()) {
            // 未登录用户不需要检查禁言（由其他拦截器处理登录检查）
            return joinPoint.proceed();
        }

        // 2. 从 Redis 检查用户是否被禁言
        if (muteService.isMuted(userId)) {
            // 3. 获取剩余禁言时间
            long remainingMinutes = muteService.getMuteRemainingMinutes(userId);
            
            // 4. 构建提示消息
            String message;
            if (remainingMinutes >= 1440) {
                // 超过24小时，显示天数
                long days = remainingMinutes / 1440;
                long hours = (remainingMinutes % 1440) / 60;
                message = String.format("%s，剩余时长 %d 天 %d 小时", checkMute.message(), days, hours);
            } else if (remainingMinutes >= 60) {
                // 超过1小时，显示小时和分钟
                long hours = remainingMinutes / 60;
                long minutes = remainingMinutes % 60;
                message = String.format("%s，剩余时长 %d 小时 %d 分钟", checkMute.message(), hours, minutes);
            } else {
                // 少于1小时，只显示分钟
                message = String.format("%s，剩余时长 %d 分钟", checkMute.message(), remainingMinutes);
            }
            
            log.warn("用户 {} 被禁言，拦截操作: {}", userId, joinPoint.getSignature().getName());
            
            // 5. 抛出业务异常
            throw new BusinessException("403", message);
        }

        // 6. 未被禁言，继续执行原方法
        return joinPoint.proceed();
    }
}
