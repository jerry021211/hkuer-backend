package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.mapper.userface.ViewCountFlushBatchMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewCountBatchWriterTest {

    @Mock
    private ViewCountFlushBatchMapper batchMapper;
    @Mock
    private PostMapper postMapper;

    private ViewCountBatchWriter batchWriter;

    @BeforeEach
    void setUp() {
        batchWriter = new ViewCountBatchWriter(batchMapper, postMapper);
    }

    @Test
    void appliesEveryIncrementForANewBatch() {
        Map<String, Long> increments = new LinkedHashMap<>();
        increments.put("100", 3L);
        increments.put("200", 5L);
        when(batchMapper.insertIgnore("batch-1", 2, 8L)).thenReturn(1);

        batchWriter.apply("batch-1", increments);

        verify(postMapper).incrementViewCount(100L, 3L);
        verify(postMapper).incrementViewCount(200L, 5L);
    }

    @Test
    void retryOfCommittedBatchDoesNotApplyIncrementsAgain() {
        Map<String, Long> increments = Map.of("100", 3L);
        when(batchMapper.insertIgnore("batch-1", 1, 3L)).thenReturn(0);

        batchWriter.apply("batch-1", increments);

        verify(postMapper, never()).incrementViewCount(100L, 3L);
    }
}
