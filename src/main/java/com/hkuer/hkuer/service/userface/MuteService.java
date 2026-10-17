package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.VO.MutedUserResponse;

import java.util.List;

/**
 * 禁言服务接口
 */
public interface MuteService {
    
    /**
     * 检查用户是否被禁言（从Redis检查）
     * 
     * @param userId 用户ID
     * @return true-被禁言, false-正常
     */
    boolean isMuted(String userId);
    
    /**
     * 获取用户禁言剩余时间（分钟）
     * 
     * @param userId 用户ID
     * @return 剩余分钟数，未禁言返回0
     */
    long getMuteRemainingMinutes(String userId);
    
    /**
     * 禁言用户
     * 
     * @param userId 用户ID
     * @param days   禁言天数
     * @return 操作结果
     */
    Result muteUser(String userId, int days);
    
    /**
     * 解除禁言
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    Result unmuteUser(String userId);
    
    /**
     * 同步禁言状态到Redis（用于登录时补偿）
     * 检查数据库状态，如果禁言未过期但Redis中没有，则重新设置Redis
     * 如果禁言已过期，则自动解除禁言
     * 
     * @param userId 用户ID
     */
    void syncMuteStatusToRedis(String userId);
    
    /**
     * 获取所有被禁言的用户列表
     * 
     * @return 被禁言用户列表
     */
    List<MutedUserResponse> getMutedUsers();
}
