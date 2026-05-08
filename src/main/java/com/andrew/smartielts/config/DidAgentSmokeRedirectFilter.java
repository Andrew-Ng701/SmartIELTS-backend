package com.andrew.smartielts.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DidAgentSmokeRedirectFilter implements Filter {

    private static final String ROOT_SMOKE_PATH = "/did-agent-smoke.html";
    private static final String API_SMOKE_PATH = "/api/did-agent-smoke.html";

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest
                && response instanceof HttpServletResponse httpResponse
                && ROOT_SMOKE_PATH.equals(httpRequest.getRequestURI())) {
            httpResponse.sendRedirect(API_SMOKE_PATH);
            return;
        }

        chain.doFilter(request, response);
    }
}
