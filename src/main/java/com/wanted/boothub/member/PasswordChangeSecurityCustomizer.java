package com.wanted.boothub.member;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * mustChangePassword 강제 이동 필터를 모든 보안 필터 체인(앱 체인, local H2 콘솔 체인)에 넣는다.
 *
 * Spring Security 7 은 Customizer&lt;HttpSecurity&gt; 빈을 SecurityFilterChain 을 만들기 전
 * 모든 HttpSecurity 에 자동으로 적용한다. 그래서 global 의 SecurityConfig 는 이 필터를 몰라도 된다.
 * 필터 자체는 빈으로 만들지 않는다 (서블릿 필터로 한 번 더 등록되는 것을 막기 위해. PasswordChangeRequiredFilter 참고).
 */
@Configuration
public class PasswordChangeSecurityCustomizer {

    @Bean
    public Customizer<HttpSecurity> passwordChangeRequired() {
        return http -> http.addFilterBefore(new PasswordChangeRequiredFilter(), AuthorizationFilter.class);
    }
}
