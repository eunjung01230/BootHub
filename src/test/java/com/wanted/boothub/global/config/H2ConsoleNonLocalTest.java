package com.wanted.boothub.global.config;

import com.wanted.boothub.member.LoginUserDetailsService;
import com.wanted.boothub.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * local 이 아닌 환경(운영 등)에는 H2 콘솔이 아예 등록되지 않는다.
 * 콘솔 서블릿도, 전용 보안 체인(CSRF 제외)도 없으므로 /h2-console 은 일반 URL 처럼 다뤄진다.
 *
 * local 프로필 없이 별도 컨텍스트를 띄운다. 다른 테스트가 공유하는 메모리 DB(boothub-test)를
 * create-drop 으로 지우지 않도록 DB 이름을 따로 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:boothub-test-nonlocal;DB_CLOSE_DELAY=-1",
        "boothub.admin.id=" + IntegrationTest.ADMIN_ID,
        "boothub.admin.password=" + IntegrationTest.ADMIN_PASSWORD,
        "boothub.links.slack=",
        "boothub.links.github=",
        "boothub.links.notion="
})
class H2ConsoleNonLocalTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Autowired
    LoginUserDetailsService loginUserDetailsService;

    static boolean hasH2ConsoleServlet(ApplicationContext context) {
        return context.getBeansOfType(ServletRegistrationBean.class).values().stream()
                .anyMatch(registration -> registration.getServlet().getClass().getName().startsWith("org.h2."));
    }

    @Test
    void H2_콘솔_서블릿과_전용_보안_체인이_등록되지_않는다() {
        assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("local");
        assertThat(context.containsBean("h2ConsoleFilterChain")).isFalse();
        assertThat(hasH2ConsoleServlet(context)).isFalse();
    }

    @Test
    void 로그인하지_않으면_다른_화면처럼_로그인으로_보낸다() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void 관리자라도_H2_콘솔은_없다() throws Exception {
        mockMvc.perform(get("/h2-console/").with(user(loginUserDetailsService.loadUserByUsername(IntegrationTest.ADMIN_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    void H2_콘솔_경로도_CSRF_검사를_받는다() throws Exception {
        mockMvc.perform(post("/h2-console/login.do")
                        .with(user(loginUserDetailsService.loadUserByUsername(IntegrationTest.ADMIN_ID))))
                .andExpect(status().isForbidden());
    }
}
