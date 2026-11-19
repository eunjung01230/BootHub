package com.wanted.boothub.member;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * mustChangePassword 인 사용자는 비밀번호를 바꾸기 전까지 /account/password 로만 보낸다.
 * 로그아웃·정적 리소스·오류 화면은 막지 않는다.
 *
 * SecurityConfig 에서 보안 필터 체인에만 넣는다. (@Component 로 등록하면 서블릿 필터로도
 * 한 번 더 등록되므로 빈으로 만들지 않는다)
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    public static final String CHANGE_URL = "/account/password";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (mustChangePassword() && !isAllowed(pathOf(request))) {
            response.sendRedirect(request.getContextPath() + CHANGE_URL);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean mustChangePassword() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof LoginUser loginUser
                && loginUser.isMustChangePassword();
    }

    private boolean isAllowed(String path) {
        return path.equals(CHANGE_URL)
                || path.equals("/logout")
                || path.equals("/login")
                || path.equals("/error")
                || path.startsWith("/css/");
    }

    private String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
