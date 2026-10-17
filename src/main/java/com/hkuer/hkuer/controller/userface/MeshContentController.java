package com.hkuer.hkuer.controller.userface;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.MeshContentRequest;
import com.hkuer.hkuer.service.userface.MeshContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/mesh") @RequiredArgsConstructor
public class MeshContentController {
    private final MeshContentService service;
    private final com.hkuer.hkuer.mapper.userface.UserMapper users;
    @GetMapping("/public/session") public Result session(){
        String id=com.hkuer.hkuer.utils.UserContext.getCurrentUserId();
        var user=id==null?null:users.selectById(id);
        if(user==null)return Result.success(null);
        java.util.Map<String,Object> data=new java.util.HashMap<>();data.put("userId",user.getId());data.put("nickname",user.getNickname());data.put("avatar",user.getAvatar());data.put("usertype",user.getUsertype());
        return Result.success(data);
    }
    @GetMapping("/public/home") public Result home(){return Result.success(service.home());}
    @GetMapping("/public/pages/{page}/feedback") public Result feedback(@PathVariable String page){return Result.success(service.pageFeedback(page));}
    @PostMapping("/pages/{page}/feedback") public Result feedback(@PathVariable String page,@RequestBody InteractionRequest r){service.pageFeedback(page,r.enabled());return Result.success(service.pageFeedback(page));}
    @GetMapping("/public/content") public Result feed(@RequestParam(defaultValue="") String kind,@RequestParam(defaultValue="") String keyword,
        @RequestParam(defaultValue="") String tag,@RequestParam(defaultValue="") String category,@RequestParam(defaultValue="") String author,@RequestParam(defaultValue="") String region,@RequestParam(required=false) Long courseId,
        @RequestParam(defaultValue="latest") String sort,@RequestParam(defaultValue="1") int current,@RequestParam(defaultValue="20") int size){return Result.success(service.feed(kind,keyword,tag,category,author,region,courseId,sort,current,size));}
    @GetMapping("/public/content/{id}") public Result detail(@PathVariable long id){return Result.success(service.detail(id));}
    @PostMapping("/content") public Result create(@RequestBody MeshContentRequest request){return service.create(request);}
    @GetMapping("/content/{id}/edit") public Result editable(@PathVariable long id){return Result.success(service.editable(id));}
    @PostMapping("/content/{id}/edit") public Result update(@PathVariable long id,@RequestBody MeshContentRequest request){return service.update(id,request);}
    public record StatusRequest(String status){}
    @PostMapping("/content/{id}/status") public Result status(@PathVariable long id,@RequestBody StatusRequest r){service.status(id,r.status());return Result.success();}
    @PostMapping("/market/batch") public Result batch(@RequestBody java.util.List<MeshContentRequest> r,@RequestHeader(value="Idempotency-Key",required=false)String key){return Result.success(service.batch(r,key));}
    @GetMapping("/public/rental/{id}/matches")public Result matches(@PathVariable long id){return Result.success(service.matches(id));}
    @GetMapping("/public/questions/{id}/answers") public Result answers(@PathVariable long id){return Result.success(service.answers(id));}
    public record AnswerRequest(String content,boolean anonymous){}
    @PostMapping("/answers/{id}/edit") public Result updateAnswer(@PathVariable long id,@RequestBody AnswerRequest r){service.updateAnswer(id,r.content(),r.anonymous());return Result.success();}
    public record InteractionRequest(String kind,boolean enabled){}
    @PostMapping("/questions/{id}/answers") public Result answer(@PathVariable long id,@RequestBody AnswerRequest r){return Result.success(service.answer(id,r.content(),r.anonymous()));}
    @PostMapping("/questions/{id}/interaction") public Result interact(@PathVariable long id,@RequestBody InteractionRequest r){service.interact(id,r.kind(),r.enabled());return Result.success();}
    @PostMapping("/answers/{id}/interaction") public Result answerAction(@PathVariable long id,@RequestBody InteractionRequest r){service.answerAction(id,r.kind(),r.enabled());return Result.success();}
}
