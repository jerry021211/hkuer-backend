package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.service.userface.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/do")
    public Result doFavorite(@RequestParam Long targetId) {
        return favoriteService.favorite(targetId);
    }

    @PostMapping("/undo")
    public Result undoFavorite(@RequestParam Long targetId) {
        return favoriteService.unfavorite(targetId);
    }

    @GetMapping("/is-favorited")
    public Result isFavorited(@RequestParam Long targetId) {
        return favoriteService.isFavorited(targetId);
    }
}

