package com.hkuer.hkuer.controller.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.BoardInsertRequest;
import com.hkuer.hkuer.service.userface.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Board")
public class BoardController {
    @Autowired
    private BoardService boardService;

    @PostMapping("/insert")
    public Result insert(@RequestBody BoardInsertRequest request) {
        Result result = boardService.insertBoard(request);
        return result;
    }
}
