package com.fourarcade.arcadebackend.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        jwtTokenProvider.resolveFromAuthorization(authorizationHeader)
                .ifPresent(token -> {
                    try {
                        Claims claims = jwtTokenProvider.getClaims(token);
                        UUID userId = jwtTokenProvider.getUserId(token);
                        String email = claims.get("email", String.class);

                        CustomUserPrincipal principal = new CustomUserPrincipal(userId, email);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        principal.getAuthorities()
                                );

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                    } catch (JwtException | IllegalArgumentException e) {
                        SecurityContextHolder.clearContext();
                    }
                });

        filterChain.doFilter(request, response);
    }
}