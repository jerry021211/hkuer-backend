package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.BoardInsertRequest;
import com.hkuer.hkuer.entity.PO.Board;
import com.hkuer.hkuer.mapper.userface.BoardMapper;
import com.hkuer.hkuer.service.userface.BoardService;
import com.hkuer.hkuer.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    @org.springframework.beans.factory.annotation.Autowired
    private SnowflakeIdGenerator idGenerator;
    private final BoardMapper boardMapper;

    @Override
    @Transactional
    public Result insertBoard(BoardInsertRequest request){

        Long boardId = idGenerator.nextId();
        Board board = new Board();
        board.setId(boardId);
        board.setSlug(request.getSlug());
        board.setName(request.getName());
        boardMapper.insert(board);
        return Result.success("上传成功");
    }

}
