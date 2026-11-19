package com.wanted.boothub.member;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그인 성공 후 이동. 비밀번호 변경이 필요한 계정은 곧바로 /account/password 로,
 * 나머지는 원래 가려던 화면(없으면 홈)으로 보낸다.
 *
 * 빈으로 등록해 두면 SecurityConfig 가 AuthenticationSuccessHandler 타입으로 주입받는다
 * (global 이 member 클래스를 직접 알지 않게 하기 위해).
 */
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public LoginSuccessHandler() {
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        if (authentication.getPrincipal() instanceof LoginUser loginUser && loginUser.isMustChangePassword()) {
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, PasswordChangeRequiredFilter.CHANGE_URL);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
