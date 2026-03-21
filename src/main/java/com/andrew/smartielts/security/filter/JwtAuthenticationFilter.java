package com.andrew.smartielts.security.filter;

import com.andrew.smartielts.security.model.LoginUser;
import com.andrew.smartielts.security.properties.JwtProperties;
import com.andrew.smartielts.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.startsWith("/api/swagger-ui")
                || uri.startsWith("/api/v3/api-docs")
                || uri.equals("/api/swagger-ui.html")
                || uri.equals("/api/doc.html")
                || uri.startsWith("/api/webjars")
                || uri.startsWith("/api/swagger-resources")
                || uri.equals("/api/auth/login")
                || uri.equals("/api/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);

            Object userIdObj = claims.get("userId");
            Object roleObj = claims.get("role");

            if (userIdObj == null || roleObj == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Long userId = Long.valueOf(userIdObj.toString());
            String role = roleObj.toString();

            List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            LoginUser loginUser = new LoginUser(userId, role, authorities);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}