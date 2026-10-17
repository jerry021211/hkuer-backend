package com.hkuer.hkuer.config;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDateTime;

@Configuration @Profile("mesh")
public class MeshDevSeed {
    @Bean ApplicationRunner meshAdmin(UserMapper users, com.hkuer.hkuer.mapper.userface.MeshAccountMapper badges,@Value("${mesh.dev.admin-password}") String password) {
        return args -> {
            if(users.selectOne(new QueryWrapper<User>().eq("username","mesh_admin"))!=null){badges.award("mesh-admin","GENESIS");return;}
            User user=new User();user.setId("mesh-admin");user.setUsername("mesh_admin");user.setEmail("admin@mesh.local");
            user.setNickname("开发管理员");user.setPassword(new BCryptPasswordEncoder().encode(password));
            user.setUsertype(1);user.setStatus(0);user.setCreatedAt(LocalDateTime.now());users.insert(user);badges.award(user.getId(),"GENESIS");
        };
    }
}
