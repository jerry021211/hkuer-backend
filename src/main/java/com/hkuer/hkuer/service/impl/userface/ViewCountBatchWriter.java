package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.mapper.userface.ViewCountFlushBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewCountBatchWriter {

    private final ViewCountFlushBatchMapper batchMapper;
    private final PostMapper postMapper;

    @Transactional(rollbackFor = Exception.class)
    public void apply(String batchId, Map<String, Long> increments) {
        long totalDelta = increments.values().stream().mapToLong(Long::longValue).sum();
        int inserted = batchMapper.insertIgnore(batchId, increments.size(), totalDelta);
        if (inserted == 0) {
            return; // 该批次已经提交过
        }

        for (Map.Entry<String, Long> entry : increments.entrySet()) {
            postMapper.incrementViewCount(Long.valueOf(entry.getKey()), entry.getValue());
        }
    }
}
