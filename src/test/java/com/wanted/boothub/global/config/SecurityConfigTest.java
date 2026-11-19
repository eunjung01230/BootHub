package com.wanted.boothub.global.config;

import com.wanted.boothub.support.IntegrationTest;
import com.wanted.boothub.member.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 로그인하지_않으면_로그인_페이지로_이동한다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 로그인_페이지는_누구나_열_수_있다() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void 시드_계정으로_로그인하면_세션에_비밀번호_해시가_남지_않는다() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("student1").password("1234"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withAuthentication(auth -> {
                    LoginUser loginUser = (LoginUser) auth.getPrincipal();
                    assertThat(loginUser.getName()).isEqualTo("김민준");
                    assertThat(loginUser.getPassword()).isNull();
                }));
    }

    @Test
    void 환경변수로_받은_관리자_계정으로_로그인한다() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("loginId")
                        .user(IntegrationTest.ADMIN_ID).password(IntegrationTest.ADMIN_PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void 예전_하드코딩_관리자_계정은_없다() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("admin").password("1234"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser
    void 헤더_외부_링크는_설정값을_쓰고_비어_있으면_비활성이다() throws Exception {
        mockMvc.perform(get("/coming-soon"))
                .andExpect(content().string(containsString("href=\"https://slack.example.test\"")))
                .andExpect(content().string(containsString("href=\"https://github.example.test\"")))
                .andExpect(content().string(containsString("class=\"header-link is-disabled\"")))
                .andExpect(content().string(not(containsString("notion.so"))));
    }

    @Test
    void 비밀번호가_틀리면_로그인되지_않는다() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("student1").password("wrong"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void 수강생은_관리자_화면에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/admin/lessons")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void 강사도_수업_관리_화면에는_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/admin/lessons")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자는_관리자_화면에_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/admin/lessons")).andExpect(status().isOk());
    }

    @Test
    void H2_콘솔은_로그인하지_않으면_로그인_페이지로_이동한다() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void H2_콘솔은_관리자가_아니면_막힌다() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(status().isForbidden());
    }
}
