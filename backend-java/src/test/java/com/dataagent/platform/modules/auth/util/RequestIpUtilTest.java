package com.dataagent.platform.modules.auth.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIpUtilTest {

    @Test
    void shouldPreferTrustedForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.11, 10.0.0.1");
        request.addHeader(RequestIpUtil.CLIENT_PUBLIC_IP_HEADER, "198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        assertThat(RequestIpUtil.resolveClientIp(request)).isEqualTo("203.0.113.11");
    }

    @Test
    void shouldUseBrowserReportedPublicIpWhenOnlyLoopbackRemoteAddressExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIpUtil.CLIENT_PUBLIC_IP_HEADER, "198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        assertThat(RequestIpUtil.resolveClientIp(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void shouldReturnNullWhenOnlyLoopbackRemoteAddressExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(RequestIpUtil.resolveClientIp(request)).isNull();
    }
}
