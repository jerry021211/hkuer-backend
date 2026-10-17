package com.hkuer.hkuer.service.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.BoardInsertRequest;

public interface BoardService {
    public Result insertBoard(BoardInsertRequest request);
}
