package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.userface.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class    LikeController {

    private final LikeService likeService;

    @PostMapping("/do")
    public Result doLike(@RequestParam Long targetId) {
        return likeService.like(targetId);
    }

    @PostMapping("/undo")
    public Result undoLike(@RequestParam Long targetId) {
        return likeService.unlike(targetId);
    }

    @GetMapping("/is-liked")
    public Result isLiked(@RequestParam Long targetId) {
        return likeService.isLiked(targetId);
    }
}


