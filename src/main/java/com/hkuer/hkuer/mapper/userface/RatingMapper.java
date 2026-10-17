package com.hkuer.hkuer.mapper.userface;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkuer.hkuer.entity.PO.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    void insertOrUpdateBatch(@Param("list") List<Rating> list);
}
