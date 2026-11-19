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
 * 로그아웃·정적 리소스(SecurityConfig.STATIC_RESOURCES)·오류 화면은 막지 않는다.
 *
 * PasswordChangeSecurityCustomizer 가 모든 보안 필터 체인의 AuthorizationFilter 앞에 넣는다. 권한 검사보다 먼저 돌아야
 * 관리자 URL 을 직접 입력해도 403 이 아니라 변경 화면으로 보내진다.
 * 경로는 정확히 일치(또는 정적 리소스 접두사)할 때만 통과시키고, "/css/../admin" 같은 경로는
 * 그보다 앞선 StrictHttpFirewall 이 거부한다.
 *
 * (@Component 로 등록하면 서블릿 필터로도 한 번 더 등록되므로 빈으로 만들지 않는다)
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
                || isStaticResource(path);
    }

    /** SecurityConfig.STATIC_RESOURCES 와 같은 범위 */
    private boolean isStaticResource(String path) {
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/favicon.ico");
    }

    private String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
