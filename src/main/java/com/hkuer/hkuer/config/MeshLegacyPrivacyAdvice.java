package com.hkuer.hkuer.config;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.VO.PostResponse;
import com.hkuer.hkuer.mapper.userface.MeshContentMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import lombok.RequiredArgsConstructor;

/** Anonymous posts must remain anonymous through the legacy API too. */
@Profile("mesh") @RestControllerAdvice @RequiredArgsConstructor
public class MeshLegacyPrivacyAdvice implements ResponseBodyAdvice<Object> {
 private final MeshContentMapper mapper;
 public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> converter){return true;}
 public Object beforeBodyWrite(Object body,MethodParameter p,MediaType type,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest request,ServerHttpResponse response){mask(body);return body;}
 private void mask(Object value){
  if(value instanceof Result result)mask(result.getData());
  else if(value instanceof IPage<?> page)page.getRecords().forEach(this::mask);
  else if(value instanceof Iterable<?> list)list.forEach(this::mask);
  else if(value instanceof PostResponse post){
   if(mapper.anonymous(post.getId())>0){post.setAuthorNickname("匿名用户");post.setAuthorAvatar(null);}
  }
 }
}
