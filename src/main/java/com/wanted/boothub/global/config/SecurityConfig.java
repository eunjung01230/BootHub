package com.wanted.boothub.global.config;

import com.wanted.boothub.member.LoginSuccessHandler;
import com.wanted.boothub.member.PasswordChangeRequiredFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * 폼 로그인 + URL 권한. 로그인 = 열람 허가, 관리 화면만 역할로 제한한다.
 *
 * 이중 잠금: URL 규칙(여기)과 별개로 관리용 Service 메서드에도 @PreAuthorize 를 붙인다
 * (@EnableMethodSecurity). URL 규칙이 실수로 빠져도 Service 에서 한 번 더 막힌다.
 *
 * 비밀번호 변경이 필요한 계정(mustChangePassword)은 로그인 직후와 이후 모든 요청에서
 * /account/password 로 보낸다. 권한 검사보다 먼저 돌려서, 권한이 있는 URL 을 직접 입력해도 우회되지 않는다.
 *
 * 로그인 세션은 SessionRegistry 에 기록한다. 계정을 비활성화하거나 비밀번호를 초기화하면
 * MemberSessionExpirer 가 그 회원의 세션을
 * 만료시키고, 다음 요청에서 ConcurrentSessionFilter 가 로그아웃시켜 /login?expired 로 보낸다.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** 로그인 없이 열리는 정적 리소스. PasswordChangeRequiredFilter 도 같은 경로만 통과시킨다. */
    public static final String[] STATIC_RESOURCES = {"/css/**", "/js/**", "/images/**", "/favicon.ico"};

    private static final String H2_CONSOLE = "/h2-console/**";

    /**
     * 개발용 H2 콘솔 전용 체인. local 프로필에서만 등록한다 (그 밖의 환경에는 체인도, 콘솔 서블릿도 없다 —
     * application.yaml 의 spring.h2.console.enabled 도 local 에서만 true).
     * ADMIN 으로 앱에 로그인한 뒤에만 열린다. 콘솔이 자체 폼과 iframe 을 쓰므로
     * CSRF 해제·같은 출처 프레임 허용을 이 경로에만 적용한다.
     */
    @Bean
    @Order(1)
    @Profile("local")
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http, SessionRegistry sessionRegistry)
            throws Exception {
        http
                .securityMatcher(H2_CONSOLE)
                .sessionManagement(expiredSessionsLogout(sessionRegistry))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .addFilterBefore(new PasswordChangeRequiredFilter(), AuthorizationFilter.class)
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /** 앱 전체 체인. CSRF 는 기본값(켜짐) 그대로 둔다. */
    @Bean
    @Order(2)
    public SecurityFilterChain appFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
                .sessionManagement(expiredSessionsLogout(sessionRegistry))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(STATIC_RESOURCES).permitAll()
                        .requestMatchers("/login", "/error").permitAll()
                        // 공지 작성(이후 수정·삭제도 이 아래에 둔다)은 강사도 쓴다. /admin/** 보다 먼저 와야 한다.
                        .requestMatchers("/admin/notices/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                        // 메서드 구분 없이(GET·POST 모두) 관리자만
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("loginId")
                        .successHandler(new LoginSuccessHandler())
                        .permitAll())
                .addFilterBefore(new PasswordChangeRequiredFilter(), AuthorizationFilter.class)
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }

    /**
     * 로그인 세션을 SessionRegistry 에 기록하고(동시 로그인 수 제한 없음), 만료 표시된 세션은
     * 다음 요청에서 로그아웃시켜 /login?expired 로 보낸다.
     */
    private static Customizer<SessionManagementConfigurer<HttpSecurity>> expiredSessionsLogout(
            SessionRegistry sessionRegistry) {
        return session -> session
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .expiredUrl("/login?expired");
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * 세션 소멸·세션 ID 변경(비밀번호 변경 후 changeSessionId) 이벤트를 SessionRegistry 에 알린다.
     * 없으면 끝난 세션이 기록에 남고, 바뀐 세션 ID 를 찾지 못해 끊을 수 없다.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
