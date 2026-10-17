package com.hkuer.hkuer.mapper.userface;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ViewCountFlushBatchMapper {

    @Insert("""
            INSERT IGNORE INTO view_count_flush_batch (batch_id, item_count, total_delta, applied_at)
            VALUES (#{batchId}, #{itemCount}, #{totalDelta}, NOW(3))
            """)
    int insertIgnore(@Param("batchId") String batchId,
                     @Param("itemCount") int itemCount,
                     @Param("totalDelta") long totalDelta);
}
