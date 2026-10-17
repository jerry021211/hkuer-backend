package com.hkuer.hkuer.controller.userface;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.userface.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class MeshCommentController {
 private final CommentService comments;
 @GetMapping("/mesh/public/content/{id}/comments") public Result comments(@PathVariable long id){return comments.listByPostId(id);}
}
