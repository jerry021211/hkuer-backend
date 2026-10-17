package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.config.PayConfig;
import com.hkuer.hkuer.entity.PO.PayOrder;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.mapper.userface.PayOrderMapper;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.utils.PayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayServiceImplTest {

    private static final String PID = "test-pid";
    private static final String KEY = "test-key";

    @Mock
    private PayConfig payConfig;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;

    private PayServiceImpl payService;

    @BeforeEach
    void setUp() {
        payService = new PayServiceImpl(payConfig, payOrderMapper, postMapper, userMapper);
        when(payConfig.getPid()).thenReturn(PID);
        when(payConfig.getKey()).thenReturn(KEY);
    }

    @Test
    void amountMismatchIsRecordedButDoesNotPublishPost() {
        PayOrder order = pendingPublishOrder();
        when(payOrderMapper.selectOne(any())).thenReturn(order);
        when(payOrderMapper.updateById(any(PayOrder.class))).thenReturn(1);

        String response = payService.handleNotify(signedNotify("0.01"));

        assertEquals("success", response);
        assertEquals(4, order.getStatus());
        assertEquals(0, new BigDecimal("0.01").compareTo(order.getActualAmount()));
        verify(postMapper, never()).selectByIdForUpdate(anyLong());
        verify(postMapper, never()).updateById(any(Post.class));
    }

    @Test
    void numericallyEqualAmountPublishesPostAndMarksOrderPaid() {
        PayOrder order = pendingPublishOrder();
        Post post = new Post();
        post.setId(100L);
        post.setAuthorId("user-1");
        post.setStatus(3);

        when(payOrderMapper.selectOne(any())).thenReturn(order);
        when(postMapper.selectByIdForUpdate(100L)).thenReturn(post);
        when(postMapper.updateById(any(Post.class))).thenReturn(1);
        when(payOrderMapper.updateById(any(PayOrder.class))).thenReturn(1);

        String response = payService.handleNotify(signedNotify("0.020"));

        assertEquals("success", response);
        assertEquals(1, order.getStatus());
        assertEquals(0, new BigDecimal("0.02").compareTo(order.getActualAmount()));
        verify(postMapper).updateById(any(Post.class));
    }

    private PayOrder pendingPublishOrder() {
        PayOrder order = new PayOrder();
        order.setOrderId("PAY_test_order");
        order.setTradeNo("platform-trade-1");
        order.setAmount(new BigDecimal("0.02"));
        order.setPayType("alipay");
        order.setUserId("user-1");
        order.setBusinessType("AD_POST_PUBLISH");
        order.setBusinessId(100L);
        order.setStatus(0);
        return order;
    }

    private Map<String, String> signedNotify(String money) {
        Map<String, String> params = new HashMap<>();
        params.put("pid", PID);
        params.put("type", "alipay");
        params.put("out_trade_no", "PAY_test_order");
        params.put("trade_no", "platform-trade-1");
        params.put("money", money);
        params.put("trade_status", "TRADE_SUCCESS");

        Map<String, Object> signParams = new HashMap<>();
        signParams.putAll(params);
        params.put("sign", PayUtil.generateSign(signParams, KEY));
        params.put("sign_type", "MD5");
        return params;
    }
}
