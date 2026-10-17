package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.LoginRequest;
import com.hkuer.hkuer.entity.DTO.LoginResponse;
import com.hkuer.hkuer.entity.DTO.UpdateProfileRequest;
import com.hkuer.hkuer.entity.DTO.UserInsertRequest;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.entity.VO.UserListResponse;
import com.hkuer.hkuer.entity.VO.UserMetaResponse;
import com.hkuer.hkuer.entity.VO.ProfileResponse;
import com.hkuer.hkuer.entity.VO.UserTypeResponse;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.AliyunMailService;
import com.hkuer.hkuer.service.userface.AttachmentService;
import com.hkuer.hkuer.service.userface.BanService;
import com.hkuer.hkuer.service.userface.MuteService;
import com.hkuer.hkuer.service.userface.UserService;
import com.hkuer.hkuer.utils.JwtUtil;
import com.hkuer.hkuer.utils.RSAUtils;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final com.hkuer.hkuer.mapper.userface.MeshAccountMapper meshAccountMapper;
    private final JwtUtil jwtUtil;
    private final AliyunMailService aliyunMailService;
    private final StringRedisTemplate stringRedisTemplate;
    private final AttachmentService attachmentService;
    @Lazy
    private final MuteService muteService;
    @Lazy
    private final BanService banService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Redis Key 前缀
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration; // RefreshToken 过期时间（毫秒），默认7天

    // RSA密钥对（启动时生成）
    private static KeyPair keyPair;

    static {
        try {
            keyPair = RSAUtils.generateKeyPair();
            log.info("RSA密钥对已生成");
        } catch (Exception e) {
            log.error("生成RSA密钥对失败", e);
        }
    }

    @Override
    @Transactional
    public Result insertUser(UserInsertRequest request) {
        // 验证必填字段
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BusinessException("400", "邮箱不能为空");
        }
        if (request.getVerificationCode() == null || request.getVerificationCode().trim().isEmpty()) {
            throw new BusinessException("400", "验证码不能为空");
        }

        // 检查用户名是否已存在
        User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("username", request.getUsername()));
        if (existingUser != null) {
            throw new BusinessException("400", "用户名已存在");
        }

        // 检查邮箱是否已被注册（验证码为null表示已注册）
        User existingEmail = userMapper.selectOne(new QueryWrapper<User>()
                .eq("email", request.getEmail())
                .isNull("verification_code"));
        if (existingEmail != null) {
            throw new BusinessException("400", "该邮箱已被注册");
        }

        // 验证邮箱验证码
        User userWithCode = userMapper.selectOne(new QueryWrapper<User>()
                .eq("email", request.getEmail())
                .eq("verification_code", request.getVerificationCode()));

        if (userWithCode == null) {
            throw new BusinessException("400", "验证码错误或已过期");
        }

        // 检查验证码是否过期（6分钟）
        if (userWithCode.getCreatedAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime codeCreatedAt = userWithCode.getCreatedAt();
            // 验证码有效期为6分钟
            if (codeCreatedAt.plusMinutes(6).isBefore(now)) {
                // 验证码已过期，删除临时记录
                if (userWithCode.getId() != null && userWithCode.getId().startsWith("temp_")) {
                    userMapper.deleteById(userWithCode.getId());
                }
                throw new BusinessException("400", "验证码已过期，请重新获取");
            }
        }

        // 解密密码（前端使用RSA公钥加密）
        String decryptedPassword;
        try {
            decryptedPassword = RSAUtils.decrypt(request.getPassword(), keyPair.getPrivate());
        } catch (Exception e) {
            // 🔥 核心修改：抛出异常触发回滚，而不是生吞异常返回Result
            throw new BusinessException("400", "密码必须使用RSA公钥加密");
        }

        if(decryptedPassword.length()<6||decryptedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new BusinessException("400","密码至少 6 位且不超过 72 字节");

        // 验证码验证成功，将临时记录升级为正式用户
        User user = userWithCode;
        user.setUsername(request.getUsername());
        // 使用BCrypt加密明文密码
        user.setPassword(passwordEncoder.encode(decryptedPassword));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setVerificationCode(null); // 验证成功后清空验证码，防止重复使用
        user.setStatus(0); // 设置为正常状态
        user.setUsertype(0); // 默认用户类型为0
        user.setCreatedAt(LocalDateTime.now().withNano(0));

        // 可选头像：如果传了，则保存
        if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
            user.setAvatar(request.getAvatar().trim());
        }

        // 使用 updateById 将临时记录“升级”为正式用户记录
        userMapper.updateById(user);

        // Allocate a permanent registration position under one row lock. Temporary
        // verification records and deleted accounts must not recycle early badges.
        long registrationRank = meshAccountMapper.lockRegistrationSequence() + 1;
        meshAccountMapper.registerRank(user.getId(), registrationRank);
        meshAccountMapper.advanceRegistrationSequence(registrationRank);
        if (registrationRank <= 1000) {
            meshAccountMapper.award(user.getId(), registrationRank <= 200 ? "GENESIS" : "PIONEER");
        }

        // 只有一切顺利，才返回成功
        return Result.success(user.getId());
    }

    @Override
    public Result sendVerificationCode(String email) {
        // 验证邮箱格式
        if (email == null || email.trim().isEmpty()) {
            return Result.error("400", "邮箱不能为空");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return Result.error("400", "邮箱格式不正确");
        }

        // 检查邮箱是否已被注册
        User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("email", email));
        if (existingUser != null && existingUser.getVerificationCode() == null) {
            return Result.error("400", "该邮箱已被注册");
        }

        if(!Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent("registration:cooldown:"+email.toLowerCase(java.util.Locale.ROOT),"1",60,TimeUnit.SECONDS)))return Result.error("429","请 60 秒后再发送验证码");

        // 生成6位随机验证码
        Random random = new java.security.SecureRandom();
        String verificationCode = String.format("%06d", random.nextInt(1000000));

        // 如果邮箱已存在（可能是之前发送过验证码但未注册），更新验证码和创建时间
        // 否则创建临时记录
        LocalDateTime now = LocalDateTime.now().withNano(0);
        if (existingUser != null) {
            // 更新验证码和创建时间（重新发送验证码时重置过期时间）
            userMapper.update(null, new UpdateWrapper<User>()
                    .eq("email", email)
                    .set("verification_code", verificationCode)
                    .set("created_at", now));
        } else {
            // 创建临时用户记录存储验证码（使用临时ID）
            String tempId = "temp_" + java.util.UUID.randomUUID().toString();
            User tempUser = new User();
            tempUser.setId(tempId);
            tempUser.setEmail(email);
            tempUser.setVerificationCode(verificationCode);
            tempUser.setCreatedAt(now);
            userMapper.insert(tempUser);
        }

        // 发送邮件
        String subject = "HKUER 注册验证码";
        String content = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                        "<h2 style='color: #333;'>欢迎注册 HKUER</h2>" +
                        "<p style='color: #666; font-size: 16px;'>您的注册验证码是：</p>" +
                        "<div style='background-color: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0;'>" +
                        "<span style='font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px;'>%s</span>"
                        +
                        "</div>" +
                        "<p style='color: #999; font-size: 14px;'>验证码有效期为6分钟，请勿泄露给他人。</p>" +
                        "<p style='color: #999; font-size: 14px;'>如果这不是您的操作，请忽略此邮件。</p>" +
                        "</div>",
                verificationCode);

        boolean sendSuccess = aliyunMailService.sendEmail(email, subject, content);
        if (!sendSuccess) {
            return Result.error("500", "验证码发送失败，请稍后重试");
        }

        log.info("验证码邮件已由配置的邮件服务处理，收件地址: {}", email);
        return Result.success("验证码已发送，请查收邮件");
    }

    @Override
    public Result login(LoginRequest request) {
        try {
            // 1) 必须用私钥解密前端密文
            String decryptedPassword = RSAUtils.decrypt(request.getPassword(), keyPair.getPrivate());

            // 2) 查询用户
            User user = userMapper.selectOne(new QueryWrapper<User>().isNotNull("password").and(q -> q.eq("username", request.getUsername()).or().eq("email", request.getUsername())));
            if (user == null) {
                return Result.error("401", "用户名或密码错误");
            }

            // 3) BCrypt校验明文密码
            if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
                return Result.error("401", "用户名或密码错误");
            }

            // 4) 检查封号状态（登录的第一道防线）
            // syncBanStatusToRedis 会检查数据库中的封号状态：
            // - 如果封号已过期，自动解封，返回 false（允许登录）
            // - 如果封号未过期，同步到 Redis（如果 Redis 重启过），返回 true（拒绝登录）
            if (banService.syncBanStatusToRedis(user.getId())) {
                // 用户仍被封号，获取封号结束时间
                User bannedUser = userMapper.selectById(user.getId());
                LocalDateTime banEndTime = bannedUser.getBanEndTime();
                String banEndTimeStr = banEndTime != null
                        ? banEndTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : "未知";
                return Result.error("403", "账号已被封禁，解封时间：" + banEndTimeStr);
            }

            // 5) 同步禁言状态到 Redis（补偿机制）
            // 检查数据库中的禁言状态，如果禁言未过期但 Redis 中没有 Key，则重新设置
            // 如果禁言已过期，则自动解禁
            muteService.syncMuteStatusToRedis(user.getId());

            // 6) 生成双Token
            String accessToken = jwtUtil.generateAccessToken(user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            // 7) 将 RefreshToken 存入 Redis，过期时间与 Token 一致
            String redisKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
            stringRedisTemplate.opsForValue().set(redisKey, user.getId(), refreshTokenExpiration,
                    TimeUnit.MILLISECONDS);
            log.info("RefreshToken 已存入 Redis, userId: {}", user.getId());

            // 8) 返回
            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType("bearer");
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());

            return Result.success(response);
        } catch (Exception e) {
            return Result.error("400", "密码必须使用RSA公钥加密");
        }
    }

    @Override
    public Result getPublicKey() {
        try {
            String publicKey = RSAUtils.getPublicKey(keyPair);
            return Result.success(publicKey);
        } catch (Exception e) {
            log.error("获取公钥失败", e);
            return Result.error("500", "获取公钥失败");
        }
    }

    @Override
    public Result refreshToken(String refreshToken) {
        try {
            // 1) 检查 Redis 中是否存在该 RefreshToken（用户是否已注销）
            String redisKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
            String storedUserId = stringRedisTemplate.opsForValue().get(redisKey);
            if (storedUserId == null) {
                log.warn("RefreshToken 在 Redis 中不存在，可能已注销或过期");
                return Result.error("401", "请重新登录");
            }

            // 2) 验证 Refresh Token 是否有效（JWT 本身的验证）
            if (!jwtUtil.validateToken(refreshToken) || "access".equals(jwtUtil.tokenType(refreshToken))) {
                // Token 无效，同时删除 Redis 中的记录
                stringRedisTemplate.delete(redisKey);
                return Result.error("401", "Refresh Token无效或已过期，请重新登录");
            }

            // 3) 从Token中获取用户ID
            String userId = jwtUtil.getUserIdFromToken(refreshToken);
            if (userId == null) {
                return Result.error("401", "无法从Token中获取用户信息");
            }

            // 4) 生成新的Access Token
            String newAccessToken = jwtUtil.generateAccessToken(userId);

            // 5) 返回新的Token
            LoginResponse response = new LoginResponse();
            response.setAccessToken(newAccessToken);
            response.setTokenType("bearer");
            response.setUserId(userId);

            return Result.success(response);
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            return Result.error("401", "刷新Token失败，请重新登录");
        }
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            String redisKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
            Boolean deleted = stringRedisTemplate.delete(redisKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("RefreshToken 已从 Redis 中删除");
            } else {
                log.warn("RefreshToken 在 Redis 中不存在或删除失败");
            }
        }
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    /**
     * 获取私钥（用于解密）
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    @Override
    public UserMetaResponse getUserMetaData(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        UserMetaResponse response = new UserMetaResponse();
        response.setUserId(user.getId());
        response.setNickName(user.getNickname());
        return response;
    }

    @Override
    public ProfileResponse getProfile(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        ProfileResponse response = new ProfileResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        return response;
    }

    @Override
    @Transactional
    public Result updateProfile(String userId, UpdateProfileRequest request) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("404", "用户不存在");
        }

        // 2. 参数校验
        if (request.getNickname() != null && request.getNickname().trim().isEmpty()) {
            return Result.error("400", "昵称不能为空");
        }

        // 3. 构建更新对象
        User updateUser = new User();
        updateUser.setId(userId);

        if (request.getNickname() != null) {
            updateUser.setNickname(request.getNickname().trim());
        }

        String avatarUrl = null;
        if (request.getAvatar() != null) {
            avatarUrl = request.getAvatar().trim();
            updateUser.setAvatar(avatarUrl);
        }

        // 4. 执行更新（只更新非空字段）
        int result = userMapper.updateById(updateUser);

        if (result > 0) {
            // 5. 如果更新了头像，将图片状态从 PENDING(0) 转为 ACTIVE(1)
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                attachmentService.activateByUrls(java.util.Collections.singletonList(avatarUrl));
                log.info("用户 {} 更新头像，图片已转为正式状态: {}", userId, avatarUrl);
            }
            return Result.success("更新成功");
        } else {
            return Result.error("500", "更新失败");
        }
    }

    @Override
    public UserTypeResponse getUserType(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        UserTypeResponse response = new UserTypeResponse();
        response.setUserType(user.getUsertype());
        return response;
    }

    @Override
    public Long getUserCount() {
        return userMapper.selectCount(null);
    }

    @Override
    @Transactional
    public Result updateUserType(String userId, Integer usertype) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("400", "用户ID不能为空");
        }

        if (usertype == null) {
            return Result.error("400", "用户类型参数不能为空");
        }

        // 验证用户类型值：只能是 0 或 1
        if (usertype != 0 && usertype != 1) {
            return Result.error("400", "用户类型参数错误，只能传入 0（取消管理员）或 1（设置管理员）");
        }

        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("404", "用户不存在");
        }

        // 更新用户权限：直接使用传入的值（0-普通用户，1-管理员）
        user.setUsertype(usertype);
        int updateCount = userMapper.updateById(user);

        if (updateCount > 0) {
            String message = usertype == 1 ? "已设置为管理员" : "已取消管理员权限";
            return Result.success(message);
        } else {
            return Result.error("500", "用户权限更新失败");
        }
    }

    @Override
    public Map<String, Object> getAllUsers(Integer page, Integer size, String email, String nickname) {
        // 参数校验
        if (page == null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 10;

        // 查询条件：所有非管理员用户（usertype != 1 或 usertype 为 null）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.ne("usertype", 1).or().isNull("usertype"));

        // 如果提供了邮箱关键词，添加模糊匹配条件
        if (email != null && !email.trim().isEmpty()) {
            queryWrapper.like("email", email.trim());
        }

        // 如果提供了昵称关键词，添加模糊匹配条件
        if (nickname != null && !nickname.trim().isEmpty()) {
            queryWrapper.like("nickname", nickname.trim());
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc("created_at");

        // MyBatis-Plus 分页查询
        Page<User> pageParam = new Page<>(page, size);
        Page<User> pageResult = userMapper.selectPage(pageParam, queryWrapper);

        // 转换为 VO
        List<UserListResponse> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (User user : pageResult.getRecords()) {
            UserListResponse response = new UserListResponse();
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setEmail(user.getEmail());
            response.setUsertype(user.getUsertype());
            response.setStatus(user.getStatus());
            response.setMuteEndTime(user.getMuteEndTime());
            response.setBanEndTime(user.getBanEndTime());
            response.setCreatedAt(user.getCreatedAt());

            // 设置状态描述
            String statusDesc = getStatusDescription(user, now);
            response.setStatusDesc(statusDesc);

            records.add(response);
        }

        // 构建分页返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("pages", pageResult.getPages());

        return result;
    }

    @Override
    public Map<String, Object> getAllUsersIncludingAdmin(Integer page, Integer size, String email, String nickname) {
        // 参数校验
        if (page == null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 10;

        // 查询条件：所有用户（包括管理员）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        // 如果提供了邮箱关键词，添加模糊匹配条件
        if (email != null && !email.trim().isEmpty()) {
            queryWrapper.like("email", email.trim());
        }

        // 如果提供了昵称关键词，添加模糊匹配条件
        if (nickname != null && !nickname.trim().isEmpty()) {
            queryWrapper.like("nickname", nickname.trim());
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc("created_at");

        // MyBatis-Plus 分页查询
        Page<User> pageParam = new Page<>(page, size);
        Page<User> pageResult = userMapper.selectPage(pageParam, queryWrapper);

        // 转换为 VO
        List<UserListResponse> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (User user : pageResult.getRecords()) {
            UserListResponse response = new UserListResponse();
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setEmail(user.getEmail());
            response.setUsertype(user.getUsertype());
            response.setStatus(user.getStatus());
            response.setMuteEndTime(user.getMuteEndTime());
            response.setBanEndTime(user.getBanEndTime());
            response.setCreatedAt(user.getCreatedAt());

            // 设置状态描述
            String statusDesc = getStatusDescription(user, now);
            response.setStatusDesc(statusDesc);

            records.add(response);
        }

        // 构建分页返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("pages", pageResult.getPages());

        return result;
    }

    /**
     * 获取用户状态描述
     */
    private String getStatusDescription(User user, LocalDateTime now) {
        Integer status = user.getStatus();
        if (status == null) {
            return "未知";
        }

        switch (status) {
            case 0:
                return "正常";
            case 1:
                // 禁言状态
                LocalDateTime muteEndTime = user.getMuteEndTime();
                if (muteEndTime != null && muteEndTime.isAfter(now)) {
                    long remainingMinutes = ChronoUnit.MINUTES.between(now, muteEndTime);
                    if (remainingMinutes >= 1440) {
                        long days = remainingMinutes / 1440;
                        long hours = (remainingMinutes % 1440) / 60;
                        return String.format("禁言中（剩余 %d 天 %d 小时）", days, hours);
                    } else if (remainingMinutes >= 60) {
                        long hours = remainingMinutes / 60;
                        long minutes = remainingMinutes % 60;
                        return String.format("禁言中（剩余 %d 小时 %d 分钟）", hours, minutes);
                    } else {
                        return String.format("禁言中（剩余 %d 分钟）", remainingMinutes);
                    }
                } else {
                    return "禁言已过期";
                }
            case 2:
                // 封号状态
                LocalDateTime banEndTime = user.getBanEndTime();
                if (banEndTime != null && banEndTime.isAfter(now)) {
                    long remainingMinutes = ChronoUnit.MINUTES.between(now, banEndTime);
                    if (remainingMinutes >= 1440) {
                        long days = remainingMinutes / 1440;
                        long hours = (remainingMinutes % 1440) / 60;
                        return String.format("封号中（剩余 %d 天 %d 小时）", days, hours);
                    } else if (remainingMinutes >= 60) {
                        long hours = remainingMinutes / 60;
                        long minutes = remainingMinutes % 60;
                        return String.format("封号中（剩余 %d 小时 %d 分钟）", hours, minutes);
                    } else {
                        return String.format("封号中（剩余 %d 分钟）", remainingMinutes);
                    }
                } else {
                    return "封号已过期";
                }
            default:
                return "未知状态";
        }
    }
}
