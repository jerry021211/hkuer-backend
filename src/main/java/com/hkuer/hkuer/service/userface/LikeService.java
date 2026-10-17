package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;

public interface LikeService {
    Result like(Long targetId);
    Result unlike(Long targetId);
    Result isLiked(Long targetId);
}



