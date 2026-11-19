package com.wanted.boothub.global.web;

import com.wanted.boothub.member.LoginUserDetailsService;
import com.wanted.boothub.support.IntegrationTest;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Spring Boot 기본 오류 처리(/error)가 상태 코드에 맞는 BootHub 오류 화면을 고르는지 확인한다.
 * MockMvc 는 서블릿 컨테이너처럼 /error 로 포워드하지 않으므로, 컨테이너가 넣는 오류 속성을 직접 넣어 호출한다.
 */
@IntegrationTest
class ErrorPageTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LoginUserDetailsService loginUserDetailsService;

    @Test
    void 상태_404는_레이아웃이_있는_404_화면을_보여준다() throws Exception {
        mockMvc.perform(get("/error")
                        .with(user(loginUserDetailsService.loadUserByUsername("s3-001")))
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/lessons/2026-11-01")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(view().name("error/404"))
                .andExpect(content().string(allOf(
                        containsString("페이지를 찾을 수 없어요"),
                        containsString("class=\"app-sidebar\""),
                        containsString("김민준"))));
    }

    @Test
    void 상태_500은_레이아웃이_있는_500_화면을_보여주고_예외_내용은_숨긴다() throws Exception {
        mockMvc.perform(get("/error")
                        .with(user(loginUserDetailsService.loadUserByUsername("s3-001")))
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("secret-detail"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(view().name("error/500"))
                .andExpect(content().string(allOf(
                        containsString("일시적인 오류가 발생했어요"),
                        containsString("class=\"app-sidebar\""),
                        not(containsString("secret-detail")))));
    }

    @Test
    void 상태_403은_4xx_화면을_보여준다() throws Exception {
        mockMvc.perform(get("/error")
                        .with(user(loginUserDetailsService.loadUserByUsername("s3-001")))
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(view().name("error/4xx"))
                .andExpect(content().string(containsString("접근 권한이 없어요")));
    }
}
