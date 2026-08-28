package com.dataagent.platform.modules.auth.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestIpUtil {

    public static final String CLIENT_PUBLIC_IP_HEADER = "X-Client-Public-IP";

    private static final String UNKNOWN = "unknown";
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private RequestIpUtil() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        for (String headerName : IP_HEADER_CANDIDATES) {
            String headerValue = firstForwardedIp(request.getHeader(headerName));
            if (headerValue != null) {
                return headerValue;
            }
        }

        String clientPublicIp = normalizeIp(request.getHeader(CLIENT_PUBLIC_IP_HEADER));
        if (clientPublicIp != null) {
            return clientPublicIp;
        }

        String remoteAddr = normalizeIp(request.getRemoteAddr());
        return isLoopbackIp(remoteAddr) ? null : remoteAddr;
    }

    private static String firstForwardedIp(String headerValue) {
        String normalizedHeaderValue = normalizeIp(headerValue);
        if (normalizedHeaderValue == null) {
            return null;
        }

        int separatorIndex = normalizedHeaderValue.indexOf(',');
        if (separatorIndex < 0) {
            return normalizedHeaderValue;
        }

        return normalizeIp(normalizedHeaderValue.substring(0, separatorIndex));
    }

    private static String normalizeIp(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || UNKNOWN.equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private static boolean isLoopbackIp(String value) {
        return "127.0.0.1".equals(value)
                || "::1".equals(value)
                || "0:0:0:0:0:0:0:1".equals(value);
    }
}
