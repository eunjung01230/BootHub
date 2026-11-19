package com.wanted.boothub.global.config;

import com.wanted.boothub.member.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
