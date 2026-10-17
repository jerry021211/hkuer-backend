package com.hkuer.hkuer.controller.userface;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.impl.userface.MeshAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/console/mesh") @RequiredArgsConstructor
public class MeshAdminController {
 private final MeshAdminService service;
 @GetMapping("/content")public Result list(@RequestParam(defaultValue="post")String kind,@RequestParam(defaultValue="1")int current,@RequestParam(defaultValue="20")int size){return Result.success(service.list(kind,current,size));}
 public record ModerationRequest(String kind,long id,int status,String reason){}
 public record AnnouncementRequest(String title,String content,Boolean pinned,String category){}
 @PostMapping("/content/status")public Result status(@RequestBody ModerationRequest r){service.status(r.kind(),r.id(),r.status(),r.reason());return Result.success();}
 @PostMapping("/announcements")public Result announcement(@RequestBody AnnouncementRequest r){return Result.success(service.announcement(r.title(),r.content(),Boolean.TRUE.equals(r.pinned()),r.category()));}
}
