package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.LoginRequest;
import com.hkuer.hkuer.entity.DTO.UpdateProfileRequest;
import com.hkuer.hkuer.entity.DTO.UserInsertRequest;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.entity.VO.UserListResponse;
import com.hkuer.hkuer.entity.VO.UserMetaResponse;
import com.hkuer.hkuer.entity.VO.ProfileResponse;
import com.hkuer.hkuer.entity.VO.UserTypeResponse;

import java.util.Map;

public interface UserService {
    Result insertUser(UserInsertRequest request);

    /**
     * 发送邮箱验证码
     */
    Result sendVerificationCode(String email);

    /**
     * 用户登录
     */
    Result login(LoginRequest request);

    /**
     * 获取RSA公钥
     */
    Result getPublicKey();

    /**
     * 刷新Token
     */
    Result refreshToken(String refreshToken);

    /**
     * 退出登录（删除 Redis 中的 RefreshToken）
     */
    void logout(String refreshToken);

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    UserMetaResponse getUserMetaData(String userId);

    /**
     * 获取当前用户的资料（昵称和头像）
     *
     * @param userId 用户ID
     * @return 用户资料信息
     */
    ProfileResponse getProfile(String userId);

    /**
     * 更新用户资料（昵称和头像）
     *
     * @param userId  用户ID
     * @param request 更新请求（包含 nickname 和 avatar）
     * @return 操作结果
     */
    Result updateProfile(String userId, UpdateProfileRequest request);

    UserTypeResponse getUserType(String userId);

    /**
     * 获取用户总数
     */
    Long getUserCount();

    /**
     * 更改用户权限
     * 
     * @param userId   用户ID
     * @param usertype 用户类型：0-取消管理员，1-设置管理员
     * @return 操作结果
     */
    Result updateUserType(String userId, Integer usertype);

    /**
     * 获取所有用户信息列表（分页，不包括管理员）
     * 
     * @param page     页码（从1开始）
     * @param size     每页数量
     * @param email    邮箱关键词（可选，模糊匹配）
     * @param nickname 昵称关键词（可选，模糊匹配）
     * @return 分页结果（包含 records, total, page, size, pages）
     */
    Map<String, Object> getAllUsers(Integer page, Integer size, String email, String nickname);

    /**
     * 获取所有用户信息列表（分页，包括管理员）
     * 
     * @param page     页码（从1开始）
     * @param size     每页数量
     * @param email    邮箱关键词（可选，模糊匹配）
     * @param nickname 昵称关键词（可选，模糊匹配）
     * @return 分页结果（包含 records, total, page, size, pages）
     */
    Map<String, Object> getAllUsersIncludingAdmin(Integer page, Integer size, String email, String nickname);
}