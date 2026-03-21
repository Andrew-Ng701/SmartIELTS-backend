package com.andrew.smartielts.common.debug;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RequestNegotiationDebugFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.contains("/user/writing/questions/") || !uri.endsWith("/submit");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // #region agent log
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", request.getMethod());
        data.put("uri", request.getRequestURI());
        data.put("query", request.getQueryString());
        data.put("contentType", request.getContentType());
        data.put("accept", request.getHeader("Accept"));
        data.put("userAgent", request.getHeader("User-Agent"));
        DebugNdjsonLogger.log("pre-fix", "H1", "RequestNegotiationDebugFilter.java:doFilterInternal", "incoming request headers", data);
        // #endregion

        try {
            filterChain.doFilter(request, response);
        } finally {
            // #region agent log
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", response.getStatus());
            out.put("respContentType", response.getContentType());
            DebugNdjsonLogger.log("pre-fix", "H1", "RequestNegotiationDebugFilter.java:doFilterInternal", "outgoing response", out);
            // #endregion
        }
    }
}

