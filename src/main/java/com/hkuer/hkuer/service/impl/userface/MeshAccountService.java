package com.hkuer.hkuer.service.impl.userface;
import com.hkuer.hkuer.mapper.userface.*;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.service.userface.AliyunMailService;
import com.hkuer.hkuer.utils.*;
import com.hkuer.hkuer.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.Duration;
import java.security.SecureRandom;
@Service @RequiredArgsConstructor
public class MeshAccountService {
 private final UserMapper users;private final UserServiceImpl auth;private final MeshAccountMapper mapper;private final AliyunMailService mail;private final StringRedisTemplate redis;
 private String actor(){String user=UserContext.getCurrentUserId();if(user==null)throw new BusinessException("401","请先登录");return user;}
 private String email(String value){if(value==null||value.length()>254||!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))throw new BusinessException("400","邮箱格式不正确");return value.trim().toLowerCase(Locale.ROOT);}
 private String key(String purpose,String email){return "verification:"+purpose+":"+email;}
 public void send(String input,boolean hku){String email=email(input);String purpose=hku?"hku:"+actor():"reset";if(hku&&!email.endsWith("@hku.hk")&&!email.endsWith("@connect.hku.hk"))throw new BusinessException("400","请使用港大邮箱");String key=key(purpose,email);if(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key+":cooldown","1",Duration.ofSeconds(60))))throw new BusinessException("429","请稍后再试");if(!hku&&users.selectOne(new QueryWrapper<User>().eq("email",email).isNotNull("password"))==null)return;String code=String.format("%06d",new SecureRandom().nextInt(1000000));redis.opsForValue().set(key,code,Duration.ofMinutes(6));redis.delete(key+":attempts");if(!mail.sendEmail(email,hku?"HKUer 邮箱认证":"HKUer 重置密码","<p>验证码：<strong>"+code+"</strong></p><p>6 分钟内有效，请勿分享。</p>")){redis.delete(key);throw new BusinessException("503","邮件发送失败，请稍后重试");}}
 private void consume(String purpose,String email,String code){String key=key(purpose,email);Long attempts=redis.opsForValue().increment(key+":attempts");redis.expire(key+":attempts",Duration.ofMinutes(6));if(attempts!=null&&attempts>5){redis.delete(key);throw new BusinessException("429","验证码尝试次数过多，请重新获取");}String actual=redis.opsForValue().get(key);if(actual==null||!actual.equals(code))throw new BusinessException("400","验证码错误或已过期");Boolean deleted=redis.delete(key);if(!Boolean.TRUE.equals(deleted))throw new BusinessException("400","验证码已使用");}
 @Transactional public void reset(String input,String code,String encrypted){String email=email(input);String password;try{password=RSAUtils.decrypt(encrypted,auth.getKeyPair().getPrivate());}catch(Exception e){throw new BusinessException("400","密码必须使用RSA加密");}if(password.length()<6||password.length()>72)throw new BusinessException("400","密码需为 6–72 位");consume("reset",email,code);User user=users.selectOne(new QueryWrapper<User>().eq("email",email).isNotNull("password"));if(user==null)throw new BusinessException("400","验证码错误或已过期");user.setPassword(new BCryptPasswordEncoder().encode(password));users.updateById(user);redis.opsForValue().set("auth:password-changed:"+user.getId(),Long.toString(System.currentTimeMillis()),Duration.ofDays(8));}
 @Transactional public void verify(String input,String code){String user=actor(),email=email(input);if(!email.endsWith("@hku.hk")&&!email.endsWith("@connect.hku.hk"))throw new BusinessException("400","请使用港大邮箱");consume("hku:"+user,email,code);mapper.verify(user,email);mapper.award(user,"HKU");}
 public List<Map<String,Object>> badges(String user){return mapper.badges(user);}
 public void wear(String code){String user=actor();if(code!=null&&!code.isBlank()&&mapper.owns(user,code)==0)throw new BusinessException("403","尚未获得该徽章");mapper.wear(user,code==null||code.isBlank()?null:code);}
}
