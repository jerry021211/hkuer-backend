package com.hkuer.hkuer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 注册拦截器、过滤器和CORS配置
 * 
 * 拦截器执行顺序：
 * 1. JwtAuthenticationFilter (Filter) - 解析Token，设置UserContext
 * 2. BanInterceptor - 检查用户是否被封号（Redis黑名单）
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final BanInterceptor banInterceptor;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 白名单路径（无需登录即可访问的接口）
     */
    private static final String[] WHITELIST_PATHS = {
            "/user/insert", // 注册
            "/user/login", // 登录
            "/user/public-key", // 获取公钥
            "/user/sendverificationcode", // 发送验证码
            "/user/refresh", // 刷新Token
            "/online/count", // 在线人数（可选）
            "/error", // 错误页面
            "/user/logout",
            "/file/upload" // 上传图片接口（允许未登录访问）
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册封号拦截器；用户身份只能来自 JwtAuthenticationFilter 解析出的 JWT。
        // 检查 Redis 黑名单，如果用户被封号，立即返回 401
        registry.addInterceptor(banInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(WHITELIST_PATHS)
                .order(1); // 优先级高
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * CORS配置，支持Cookie跨域
     * 必须使用 FilterRegistrationBean 包装并设为 HIGHEST_PRECEDENCE，
     * 让 CorsFilter 在 JwtAuthenticationFilter 之前执行，否则：
     *   1) OPTIONS 预检请求会被 JwtFilter 拦截返回 401，导致跨域请求全部失败
     *   2) JwtFilter 抛出的 401/403 响应缺失 CORS 头，浏览器会拦截 axios 拿不到错误信息
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许的源（前端地址）
        // 注意：当allowCredentials为true时，不能使用 "*"，需要指定具体域名
        // 开发环境可以使用 localhost 的不同端口
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:8080");
        config.addAllowedOrigin("http://localhost:5173"); // Vite默认端口
        config.addAllowedOrigin("http://localhost:5174");
        config.addAllowedOrigin("http://127.0.0.1:5174");
        config.addAllowedOrigin("http://127.0.0.1:3000");
        config.addAllowedOrigin("http://127.0.0.1:8080");
        config.addAllowedOrigin("http://127.0.0.1:5173");

        // === Capacitor 移动 App ===
        // Android App 的 WebView origin（http/https 都加上以兼容不同 androidScheme 配置）
        config.addAllowedOrigin("http://localhost");
        config.addAllowedOrigin("https://localhost");
        // Capacitor 自定义 scheme（iOS / 旧版 Android 默认）
        config.addAllowedOrigin("capacitor://localhost");
        config.addAllowedOrigin("ionic://localhost");

        // 生产环境添加实际的前端域名，如：
        // config.addAllowedOrigin("https://yourdomain.com");

        // 允许携带凭证（Cookie）
        config.setAllowCredentials(true);
        // 允许的请求方法
        config.addAllowedMethod("*");
        // 允许的请求头
        config.addAllowedHeader("*");
        // 允许暴露的响应头（前端可以读取的响应头）
        config.addExposedHeader("*");

        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 必须比 JwtFilter (order=1) 更早
        return bean;
    }
}
