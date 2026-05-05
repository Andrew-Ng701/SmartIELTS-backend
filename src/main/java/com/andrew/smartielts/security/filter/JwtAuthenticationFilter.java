package com.andrew.smartielts.security.filter;

import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.auth.mapper.AuthMapper;
import com.andrew.smartielts.security.model.LoginUser;
import com.andrew.smartielts.security.properties.JwtProperties;
import com.andrew.smartielts.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final AuthMapper authMapper;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/swagger-ui")
                || uri.startsWith("/api/v3/api-docs")
                || uri.equals("/api/swagger-ui.html")
                || uri.equals("/api/doc.html")
                || uri.startsWith("/api/webjars")
                || uri.startsWith("/api/swagger-resources")
                || uri.equals("/api/auth/login")
                || uri.equals("/api/auth/register")
                || uri.startsWith("/api/speaking/webhook")
                || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

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
            Object versionObj = claims.get("tokenVersion");

            if (userIdObj == null || roleObj == null || versionObj == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Long userId = Long.valueOf(userIdObj.toString());
            String role = roleObj.toString();
            Long tokenVersion = Long.valueOf(versionObj.toString());

            User user = authMapper.findActiveById(userId);
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Long dbVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
            if (!dbVersion.equals(tokenVersion)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            LoginUser loginUser = new LoginUser(userId, role, tokenVersion, authorities);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorities);

            authentication.setDetails(loginUser);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}