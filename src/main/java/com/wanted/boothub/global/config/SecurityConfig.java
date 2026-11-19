package com.wanted.boothub.global.config;

import com.wanted.boothub.member.LoginSuccessHandler;
import com.wanted.boothub.member.PasswordChangeRequiredFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * 폼 로그인 + URL 권한. 로그인 = 열람 허가, 관리 화면만 역할로 제한한다.
 * 비밀번호 변경이 필요한 계정(mustChangePassword)은 로그인 직후와 이후 모든 요청에서
 * /account/password 로 보낸다.
 */
@Configuration
public class SecurityConfig {

    private static final String H2_CONSOLE = "/h2-console/**";

    /**
     * 개발용 H2 콘솔 전용 체인. ADMIN 으로 앱에 로그인한 뒤에만 열린다.
     * 콘솔이 자체 폼과 iframe 을 쓰므로 CSRF 해제·같은 출처 프레임 허용을 이 경로에만 적용한다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(H2_CONSOLE)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/login", "/error").permitAll()
                        .requestMatchers("/admin/notices/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("loginId")
                        .successHandler(new LoginSuccessHandler())
                        .permitAll())
                .addFilterAfter(new PasswordChangeRequiredFilter(), AuthorizationFilter.class)
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
