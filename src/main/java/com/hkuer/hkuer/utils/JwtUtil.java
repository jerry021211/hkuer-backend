package com.hkuer.hkuer.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT工具类
 */
@Component
@Slf4j
public class JwtUtil {
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private org.springframework.data.redis.core.StringRedisTemplate redis;

    @Value("${jwt.secret:your-256-bit-secret-your-256-bit-secret-your-256-bit-secret-your-256-bit-secret}")
    private String secret;

    @Value("${jwt.expiration:300000}") // 默认5分钟
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-expiration:600000}") // 默认10分钟
    private Long refreshTokenExpiration;

    /**
     * 生成访问Token（短期）
     */
    public String generateAccessToken(String userId) {
        return generateToken(userId, accessTokenExpiration, "access");
    }

    /**
     * 生成刷新Token（长期）
     */
    public String generateRefreshToken(String userId) {
        return generateToken(userId, refreshTokenExpiration, "refresh");
    }

    /**
     * 生成Token（私有方法）
     */
    private String generateToken(String userId, long expirationMs, String type) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.builder()
                .subject(userId)
                .id(java.util.UUID.randomUUID().toString())
                .claim("token_type", type)
                .claim("password_version", passwordVersion(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 生成Token（兼容旧接口）
     */
    @Deprecated
    public String generateToken(String userId) {
        return generateAccessToken(userId);
    }

    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期，无法获取用户ID");
            return null;
        } catch (Exception e) {
            log.error("解析Token失败", e);
            return null;
        }
    }

    /**
     * 获取Token的过期时间
     */
    public Date getExpirationFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("获取Token过期时间失败", e);
            return null;
        }
    }

    /**
     * 获取Token的签发时间
     */
    public Date getIssuedAtFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getIssuedAt();
        } catch (Exception e) {
            log.error("获取Token签发时间失败", e);
            return null;
        }
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String version=java.util.Objects.toString(claims.get("password_version"),"0");
            if (!version.equals(passwordVersion(claims.getSubject()))) return false;
            // 检查过期时间
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                log.warn("Token已过期，过期时间: {}", expiration);
                return false;
            }

            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token验证异常", e);
            return false;
        }
    }
    private String passwordVersion(String userId) {
        if(redis==null)return "0";
        return java.util.Objects.toString(redis.opsForValue().get("auth:password-changed:"+userId),"0");
    }
    public String tokenType(String token) {
        try {return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes())).build().parseSignedClaims(token).getPayload().get("token_type",String.class);}
        catch(Exception e){return null;}
    }}
