package com.hkuer.hkuer.controller.userface;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.service.userface.MeshCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor
public class MeshCourseController {
 private final MeshCourseService service;
 @GetMapping("/mesh/public/courses") public Result list(@RequestParam(defaultValue="")String code,@RequestParam(defaultValue="")String name,@RequestParam(defaultValue="")String keyword,@RequestParam(defaultValue="")String level,@RequestParam(defaultValue="")String subject,@RequestParam(defaultValue="false")boolean mine,@RequestParam(defaultValue="code")String sort,@RequestParam(defaultValue="1")int current,@RequestParam(defaultValue="24")int size){return Result.success(service.list(code,name,keyword,level,subject,mine,sort,current,size));}
 @GetMapping("/mesh/public/courses/subjects") public Result subjects(@RequestParam(defaultValue="")String level){return Result.success(service.subjects(level));}
 @GetMapping("/mesh/public/courses/{id}") public Result detail(@PathVariable long id){return Result.success(service.detail(id));}
 @PostMapping("/mesh/courses/{id}/interaction") public Result action(@PathVariable long id,@RequestBody MeshContentController.InteractionRequest r){service.action(id,r.kind(),r.enabled());return Result.success();}
 public record VoteRequest(boolean recommended,Double rating,String comment){}
 @PostMapping("/mesh/courses/{id}/vote")public Result vote(@PathVariable long id,@RequestBody VoteRequest r){
  Double rating=r.rating();
  if(rating!=null&&(!Double.isFinite(rating)||rating!=Math.rint(rating)||rating<1||rating>5))throw new BusinessException("400","评分必须是 1–5 的整数");
  service.vote(id,r.recommended(),rating==null?null:rating.intValue(),r.comment());return Result.success();
 }
 @PostMapping("/mesh/courses/{id}/reviews") public Result review(@PathVariable long id,@RequestBody Map<String,Object> r){return Result.success(service.review(id,r));}
 @PostMapping("/mesh/courses/{id}/groups") public Result group(@PathVariable long id,@RequestBody Map<String,Object> r){return Result.success(service.group(id,r));}
 @PostMapping("/mesh/groups/{id}/membership") public Result joinGroup(@PathVariable long id,@RequestBody MeshContentController.InteractionRequest r){service.joinGroup(id,r.enabled());return Result.success();}
 @GetMapping("/mesh/groups/{id}") public Result groupDetail(@PathVariable long id){return Result.success(service.groupDetail(id));}
 @PostMapping("/mesh/groups/{id}/edit") public Result updateGroup(@PathVariable long id,@RequestBody Map<String,Object> r){service.updateGroup(id,r);return Result.success();}
 @PostMapping("/mesh/groups/{id}/members/{member}/remove") public Result removeMember(@PathVariable long id,@PathVariable String member){service.removeMember(id,member);return Result.success();}
 @PostMapping("/mesh/groups/{id}/close") public Result closeGroup(@PathVariable long id){service.closeGroup(id);return Result.success();}
 @PostMapping("/mesh/courses/{id}/materials") public Result material(@PathVariable long id,@RequestBody Map<String,Object> r){return Result.success(service.material(id,r));}
 @PostMapping("/console/mesh/courses") public Result create(@RequestBody Map<String,Object> r){return Result.success(service.create(r));}
}
