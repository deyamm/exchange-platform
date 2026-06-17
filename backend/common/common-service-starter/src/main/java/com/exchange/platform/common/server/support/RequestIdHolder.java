package com.exchange.platform.common.server.support;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.exchange.platform.common.core.util.IdGenerator;

public final class RequestIdHolder {
    private RequestIdHolder() {
    
    }

    public static String getRequestId(HttpServletRequest request) {
        // 优先从请求头中获取请求ID，如果没有则生成一个新的请求ID
        return Optional.ofNullable(
            request.getHeader("X-Request-ID"))
            .filter(v -> !v.isBlank())
            .orElseGet(() -> IdGenerator.generate("REQ"));
    }
    
}
