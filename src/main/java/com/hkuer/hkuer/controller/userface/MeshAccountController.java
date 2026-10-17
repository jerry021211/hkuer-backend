package com.hkuer.hkuer.controller.userface;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.impl.userface.MeshAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/mesh") @RequiredArgsConstructor
public class MeshAccountController {
 private final MeshAccountService service;
 public record CodeRequest(String email,String code,String password){}
 public record BadgeRequest(String code){}
 @PostMapping("/public/password/code")public Result resetCode(@RequestBody CodeRequest r){service.send(r.email(),false);return Result.success("如果邮箱已注册，验证码将发送到该邮箱");}
 @PostMapping("/public/password/reset")public Result reset(@RequestBody CodeRequest r){service.reset(r.email(),r.code(),r.password());return Result.success();}
 @PostMapping("/verification/send")public Result hkuCode(@RequestBody CodeRequest r){service.send(r.email(),true);return Result.success();}
 @PostMapping("/verification/confirm")public Result hkuVerify(@RequestBody CodeRequest r){service.verify(r.email(),r.code());return Result.success();}
 @GetMapping("/public/users/{id}/badges")public Result badges(@PathVariable String id){return Result.success(service.badges(id));}
 @PostMapping("/profile/badge")public Result wear(@RequestBody BadgeRequest r){service.wear(r.code());return Result.success();}
}
