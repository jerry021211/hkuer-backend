package com.hkuer.hkuer.config;

import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.OnlineService;
import com.hkuer.hkuer.utils.CookieUtil;
import com.hkuer.hkuer.utils.JwtUtil;
import com.hkuer.hkuer.utils.UserContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private OnlineService onlineService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void usesJwtSubjectAndIgnoresSpoofedUserIdHeader() throws Exception {
        JwtAuthenticationFilter filter = createFilter();
        MockHttpServletRequest request = request("/post/detail", "valid-token");
        request.addHeader("X-User-Id", "attacker-user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn("jwt-user");
        doAnswer(invocation -> {
            assertEquals("jwt-user", UserContext.getCurrentUserId());
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        verify(onlineService).heartbeat("jwt-user");
        verify(filterChain).doFilter(request, response);
        assertNull(UserContext.getCurrentUserId());
    }

    @Test
    void clearsUserContextWhenAdminAccessIsDenied() throws Exception {
        JwtAuthenticationFilter filter = createFilter();
        MockHttpServletRequest request = request("/console/user/list", "valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = new User();
        user.setUsertype(0);

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn("normal-user");
        when(userMapper.selectById("normal-user")).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(UserContext.getCurrentUserId());
    }

    @Test
    void clearsStaleUserContextWhenTokenIsInvalid() throws Exception {
        JwtAuthenticationFilter filter = createFilter();
        MockHttpServletRequest request = request("/post/detail", "invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserContext.setCurrentUserId("stale-user");

        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(UserContext.getCurrentUserId());
    }

    private JwtAuthenticationFilter createFilter() {
        return new JwtAuthenticationFilter(jwtUtil, cookieUtil, onlineService, userMapper);
    }

    private MockHttpServletRequest request(String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
