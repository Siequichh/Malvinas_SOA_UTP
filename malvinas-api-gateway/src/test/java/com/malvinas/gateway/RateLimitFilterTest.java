package com.malvinas.gateway;

import com.malvinas.gateway.filter.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filterWithLimit(int limit) throws Exception {
        RateLimitFilter f = new RateLimitFilter();
        Field field = RateLimitFilter.class.getDeclaredField("requestsPerMinute");
        field.setAccessible(true);
        field.setInt(f, limit);
        return f;
    }

    @Test
    void underLimit_passes() throws Exception {
        RateLimitFilter filter = filterWithLimit(5);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("1.2.3.4");

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void overLimit_returns429() throws Exception {
        RateLimitFilter filter = filterWithLimit(3);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("9.8.7.6");

        for (int i = 0; i < 3; i++) {
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void actuatorPath_exempt() throws Exception {
        RateLimitFilter filter = filterWithLimit(1);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("5.5.5.5");
        req.setRequestURI("/actuator/health");

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }
}
