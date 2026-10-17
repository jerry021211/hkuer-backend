package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;

public interface FavoriteService {
    Result favorite(Long targetId);
    Result unfavorite(Long targetId);
    Result isFavorited(Long targetId);
}

