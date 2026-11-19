package com.wanted.boothub.admin;

import com.wanted.boothub.member.LoginUserDetailsService;
import com.wanted.boothub.member.Member;
import com.wanted.boothub.member.MemberRepository;
import com.wanted.boothub.member.dto.IssuedPasswordDto;
import com.wanted.boothub.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 5단계 회원 관리: 계정 생성(랜덤 비밀번호 1회 표시) → 첫 로그인 변경 강제, 초기화, 비활성화.
 */
@IntegrationTest
@Transactional
class AdminMemberTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LoginUserDetailsService loginUserDetailsService;

    @Autowired
    MemberRepository memberRepository;

    private UserDetails admin() {
        return loginUserDetailsService.loadUserByUsername(IntegrationTest.ADMIN_ID);
    }

    private IssuedPasswordDto issuedOf(MvcResult result) {
        return (IssuedPasswordDto) result.getFlashMap().get("issuedPassword");
    }

    @Test
    void 관리자가_만든_계정은_랜덤_비밀번호로_로그인하고_첫_로그인에_비밀번호를_바꾼다() throws Exception {
        // 1) 계정 생성: 비밀번호는 시스템이 발급해 flash 로 한 번만 넘긴다
        MvcResult created = mockMvc.perform(post("/admin/members/new").with(user(admin())).with(csrf())
                        .param("loginId", "s3-099").param("name", "홍길동").param("role", "STUDENT"))
                .andExpect(redirectedUrl("/admin/members"))
                .andReturn();
        IssuedPasswordDto issued = issuedOf(created);
        assertThat(issued.loginId()).isEqualTo("s3-099");
        assertThat(issued.password()).hasSize(10);
        assertThat(issued.toString()).doesNotContain(issued.password());

        // 2) 목록에 한 번만 보이고, 다시 열면 보이지 않는다
        mockMvc.perform(get("/admin/members").with(user(admin())).flashAttr("issuedPassword", issued))
                .andExpect(content().string(containsString(issued.password())))
                .andExpect(content().string(containsString("홍길동 (s3-099)")));
        mockMvc.perform(get("/admin/members").with(user(admin())))
                .andExpect(content().string(containsString("s3-099")))
                .andExpect(content().string(not(containsString(issued.password()))));

        // 3) DB 에는 BCrypt 해시만 있다
        Member saved = memberRepository.findByLoginId("s3-099").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(issued.password()).startsWith("$2");
        assertThat(saved.isMustChangePassword()).isTrue();

        // 4) 첫 로그인 → 비밀번호 변경 화면, 바꾸기 전에는 다른 화면으로 갈 수 없다
        MvcResult firstLogin = mockMvc.perform(formLogin("/login").userParameter("loginId")
                        .user("s3-099").password(issued.password()))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/account/password"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) firstLogin.getRequest().getSession(false);
        mockMvc.perform(get("/").session(session)).andExpect(redirectedUrl("/account/password"));
        mockMvc.perform(get("/notices").session(session)).andExpect(redirectedUrl("/account/password"));
        mockMvc.perform(get("/css/app.css").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/account/password").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("처음 로그인했거나 비밀번호가 초기화되었어요")));

        // 5) 임시 비밀번호를 그대로 다시 쓰면 거부 (변경 강제 우회 방지)
        mockMvc.perform(post("/account/password").session(session).with(csrf())
                        .param("currentPassword", issued.password())
                        .param("newPassword", issued.password())
                        .param("newPasswordConfirm", issued.password()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("지금 쓰는 비밀번호와 다른 비밀번호를 정해 주세요.")));

        // 6) 새 비밀번호로 바꾸면 바로 홈을 쓸 수 있다
        mockMvc.perform(post("/account/password").session(session).with(csrf())
                        .param("currentPassword", issued.password())
                        .param("newPassword", "boothub2026")
                        .param("newPasswordConfirm", "boothub2026"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("flashMessage", "비밀번호를 변경했어요."));
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());
        assertThat(memberRepository.findByLoginId("s3-099").orElseThrow().isMustChangePassword()).isFalse();

        // 7) 다음 로그인부터는 새 비밀번호로, 홈으로 바로 간다
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-099").password("boothub2026"))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-099").password(issued.password()))
                .andExpect(unauthenticated());
    }

    @Test
    void 비밀번호를_초기화하면_예전_비밀번호는_막히고_새_임시_비밀번호로_변경을_강제한다() throws Exception {
        Member student = memberRepository.findByLoginId("s3-002").orElseThrow();

        MvcResult reset = mockMvc.perform(post("/admin/members/{id}/reset-password", student.getId())
                        .with(user(admin())).with(csrf()))
                .andExpect(redirectedUrl("/admin/members"))
                .andReturn();
        IssuedPasswordDto issued = issuedOf(reset);
        assertThat(issued.action()).isEqualTo("초기화");

        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-002").password("1234"))
                .andExpect(unauthenticated());
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-002").password(issued.password()))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/account/password"));
    }

    @Test
    void 비활성화한_계정은_로그인할_수_없다() throws Exception {
        Member student = memberRepository.findByLoginId("s3-003").orElseThrow();

        mockMvc.perform(post("/admin/members/{id}/deactivate", student.getId()).with(user(admin())).with(csrf()))
                .andExpect(redirectedUrl("/admin/members"))
                .andExpect(flash().attributeExists("flashMessage"));

        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-003").password("1234"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        mockMvc.perform(get("/admin/members").with(user(admin())))
                .andExpect(content().string(containsString("비활성")));
    }

    @Test
    void 관리자는_자기_계정을_비활성화할_수_없다() throws Exception {
        Member self = memberRepository.findByLoginId(IntegrationTest.ADMIN_ID).orElseThrow();

        mockMvc.perform(post("/admin/members/{id}/deactivate", self.getId()).with(user(admin())).with(csrf()))
                .andExpect(redirectedUrl("/admin/members"))
                .andExpect(flash().attribute("flashError", "본인 계정은 비활성화할 수 없어요."));

        assertThat(memberRepository.findByLoginId(IntegrationTest.ADMIN_ID).orElseThrow().isActive()).isTrue();
    }

    @Test
    void 아이디_규칙에_맞지_않거나_이미_있는_아이디는_만들_수_없다() throws Exception {
        mockMvc.perform(post("/admin/members/new").with(user(admin())).with(csrf())
                        .param("loginId", "S3_017").param("name", "홍길동").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("아이디는 영문 소문자, 숫자, 하이픈(-)으로 4~20자여야 해요.")));

        mockMvc.perform(post("/admin/members/new").with(user(admin())).with(csrf())
                        .param("loginId", "s3-001").param("name", "홍길동").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 쓰고 있는 아이디예요.")));

        mockMvc.perform(post("/admin/members/new").with(user(admin())).with(csrf())
                        .param("loginId", "s3-100").param("name", " ").param("role", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("실명을 입력해 주세요.")))
                .andExpect(content().string(containsString("역할을 골라 주세요.")));
    }

    @Test
    void 계정_생성_화면에는_비밀번호_입력칸이_없다() throws Exception {
        mockMvc.perform(get("/admin/members/new").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("type=\"password\""))));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void 관리자가_아니면_회원_관리를_할_수_없다() throws Exception {
        mockMvc.perform(get("/admin/members")).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/members/new").with(csrf())
                        .param("loginId", "s3-101").param("name", "홍길동").param("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }
}
