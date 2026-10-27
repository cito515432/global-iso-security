package com.globalisosecurity.backend.config;

import com.globalisosecurity.backend.services.RolePermissionPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Enforces role permission flags on the server, in addition to existing role and tenant checks. */
@Component
public class RolePermissionAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String required = RolePermissionPolicy.requiredPermission(request.getRequestURI(), request.getMethod());
        if (authentication != null && authentication.isAuthenticated() && required != null) {
            Set<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
            if (!RolePermissionPolicy.isAuthorized(authorities, required)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":403,\"message\":\"No tiene permisos para realizar esta operación\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
