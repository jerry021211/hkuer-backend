package com.hkuer.hkuer.mapper.userface;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkuer.hkuer.entity.PO.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {
}
