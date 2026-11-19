package com.wanted.boothub.member;

import com.wanted.boothub.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 본인 비밀번호 변경 (/account/password). 변경 강제 흐름은 AdminMemberTest 에서 본다.
 */
@IntegrationTest
@Transactional
class AccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LoginUserDetailsService loginUserDetailsService;

    private UserDetails student() {
        return loginUserDetailsService.loadUserByUsername("s3-001");
    }

    private ResultActions change(String current, String newPassword, String confirm) throws Exception {
        return mockMvc.perform(post("/account/password").with(user(student())).with(csrf())
                .param("currentPassword", current)
                .param("newPassword", newPassword)
                .param("newPasswordConfirm", confirm));
    }

    @Test
    void 변경이_필요_없는_계정도_스스로_비밀번호를_바꿀_수_있다() throws Exception {
        mockMvc.perform(get("/account/password").with(user(student())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("처음 로그인했거나"))));
        mockMvc.perform(get("/").with(user(student())))
                .andExpect(content().string(containsString("href=\"/account/password\"")));

        change("1234", "react2026", "react2026").andExpect(redirectedUrl("/"));

        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-001").password("1234"))
                .andExpect(unauthenticated());
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-001").password("react2026"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void 현재_비밀번호가_틀리면_바꾸지_않는다() throws Exception {
        change("wrong", "react2026", "react2026")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("현재 비밀번호가 맞지 않아요.")));
    }

    @Test
    void 비밀번호_규칙을_어기면_이유를_보여준다() throws Exception {
        change("1234", "short1", "short1")
                .andExpect(content().string(containsString("비밀번호는 8자 이상이어야 해요.")));
        change("1234", "onlyletters", "onlyletters")
                .andExpect(content().string(containsString("영문과 숫자를 모두 넣어 주세요.")));
        change("1234", "my-s3-001-pw", "my-s3-001-pw")
                .andExpect(content().string(containsString("아이디가 들어간 비밀번호는 쓸 수 없어요.")));
    }

    @Test
    void 새_비밀번호_확인이_다르면_바꾸지_않는다() throws Exception {
        change("1234", "react2026", "react2027")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("새 비밀번호가 서로 달라요.")));
    }

    @Test
    void 로그인하지_않으면_비밀번호_변경_화면도_로그인으로_보낸다() throws Exception {
        mockMvc.perform(get("/account/password")).andExpect(redirectedUrl("/login"));
    }
}
