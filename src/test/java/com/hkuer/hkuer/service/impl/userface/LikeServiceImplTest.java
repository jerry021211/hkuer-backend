package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.PO.Like;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.mapper.userface.LikeMapper;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.messaging.InteractionCacheService;
import com.hkuer.hkuer.messaging.InteractionEventTypes;
import com.hkuer.hkuer.messaging.InteractionOutboxService;
import com.hkuer.hkuer.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private LikeMapper likeMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private InteractionCacheService cacheService;
    @Mock
    private InteractionOutboxService outboxService;

    private LikeServiceImpl likeService;

    @BeforeEach
    void setUp() {
        likeService = new LikeServiceImpl(likeMapper, postMapper, cacheService, outboxService);
        UserContext.setCurrentUserId("user-1");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void likeWritesFactCountAndOutboxOnce() {
        Post post = new Post();
        post.setLikeNum(10L);
        when(postMapper.selectByIdForUpdate(100L)).thenReturn(post);
        when(likeMapper.insertIgnore(any(Like.class))).thenReturn(1);
        when(postMapper.incrementLikeCount(100L, 1)).thenReturn(1);

        Result result = likeService.like(100L);

        assertEquals("200", result.getCode());
        assertEquals(11L, result.getData());
        verify(postMapper).incrementLikeCount(100L, 1);
        verify(outboxService).append(
                InteractionEventTypes.POST_LIKE_CHANGED, "POST", "100", "user-1");
    }

    @Test
    void duplicateLikeDoesNotIncreaseCountOrCreateEvent() {
        Post post = new Post();
        post.setLikeNum(10L);
        when(postMapper.selectByIdForUpdate(100L)).thenReturn(post);
        when(likeMapper.insertIgnore(any(Like.class))).thenReturn(0);

        Result result = likeService.like(100L);

        assertEquals("200", result.getCode());
        assertEquals(10L, result.getData());
        verify(postMapper, never()).incrementLikeCount(anyLong(), anyLong());
        verify(outboxService, never()).append(anyString(), anyString(), anyString(), anyString());
    }
}
