package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.VO.BannedUserResponse;

import java.util.List;

/**
 * 用户封号服务接口
 * 
 * 封号定义：用户无法登录，在线用户立即强制下线，所有接口均无法访问
 */
public interface BanService {
    
    /**
     * 检查用户是否被封号（从Redis检查，高性能）
     * 
     * @param userId 用户ID
     * @return true-被封号, false-正常
     */
    boolean isBanned(String userId);
    
    /**
     * 获取用户封号剩余时间（分钟）
     * 
     * @param userId 用户ID
     * @return 剩余分钟数，未封号返回0
     */
    long getBanRemainingMinutes(String userId);
    
    /**
     * 封禁用户
     * 
     * @param userId 用户ID
     * @param days   封禁天数
     * @return 操作结果
     */
    Result banUser(String userId, int days);
    
    /**
     * 解除封禁
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    Result unbanUser(String userId);
    
    /**
     * 同步封号状态到Redis（用于登录时补偿检查）
     * 如果封号已过期，自动解封
     * 
     * @param userId 用户ID
     * @return true-用户当前被封号, false-用户正常或已自动解封
     */
    boolean syncBanStatusToRedis(String userId);
    
    /**
     * 获取所有被封号的用户列表
     * 
     * @return 被封号用户列表
     */
    List<BannedUserResponse> getBannedUsers();
}
