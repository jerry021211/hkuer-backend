package com.hkuer.hkuer.controller.userface;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.userface.MeshSocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/mesh") @RequiredArgsConstructor
public class MeshSocialController {
 private final MeshSocialService service;
 @GetMapping("/public/users/{id}")public Result profile(@PathVariable String id){return Result.success(service.profile(id));}
 @GetMapping("/public/users/{id}/content")public Result profileContent(@PathVariable String id,@RequestParam(defaultValue="public")String section,@RequestParam(defaultValue="")String filter,@RequestParam(defaultValue="1")int current,@RequestParam(defaultValue="20")int size){return Result.success(service.profileContent(id,section,filter,current,size));}
 @PostMapping("/profile")public Result profile(@RequestBody Map<String,Object> p){service.updateProfile(p);return Result.success();}
 @GetMapping("/conversations")public Result conversations(){return Result.success(service.conversations());}
 public record ConversationRequest(String peerId,long contextPostId){}
 @PostMapping("/conversations")public Result conversation(@RequestBody ConversationRequest r){return Result.success(service.conversation(r.peerId(),r.contextPostId()));}
 @GetMapping("/conversations/{id}/messages")public Result messages(@PathVariable long id,@RequestParam(defaultValue="0")long before){return Result.success(service.messages(id,before));}
 public record MessageRequest(String content,String clientId){}
 @PostMapping("/conversations/{id}/messages")public Result message(@PathVariable long id,@RequestBody MessageRequest r){return Result.success(service.message(id,r.content(),r.clientId()));}
 @PostMapping("/conversations/{id}/read")public Result read(@PathVariable long id,@RequestBody(required=false) Map<String,Long> r){service.read(id,r==null?0:r.getOrDefault("lastId",0L));return Result.success();}
 @GetMapping("/notifications")public Result notifications(@RequestParam(defaultValue="interaction")String kind,@RequestParam(defaultValue="1")int current,@RequestParam(defaultValue="20")int size){return Result.success(service.notifications(kind,current,size));}
 @PostMapping("/notifications/{id}/read")public Result readNotification(@PathVariable long id){service.readNotification(id);return Result.success();}
}
