package com.wanted.boothub.global.config;

import com.wanted.boothub.global.exception.BusinessException;
import com.wanted.boothub.lesson.LessonRepository;
import com.wanted.boothub.lesson.LessonService;
import com.wanted.boothub.lesson.ResourceType;
import com.wanted.boothub.member.LoginUser;
import com.wanted.boothub.member.LoginUserDetailsService;
import com.wanted.boothub.member.Member;
import com.wanted.boothub.member.MemberRepository;
import com.wanted.boothub.member.MemberService;
import com.wanted.boothub.member.PasswordChangeRequiredFilter;
import com.wanted.boothub.member.Role;
import com.wanted.boothub.member.dto.IssuedPasswordDto;
import com.wanted.boothub.notice.NoticeCategory;
import com.wanted.boothub.notice.NoticeRepository;
import com.wanted.boothub.notice.NoticeService;
import com.wanted.boothub.support.IntegrationTest;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * URL 직접 접근으로 권한을 우회할 수 없는지 확인하는 접근 제어 표.
 *
 *  1. URL 규칙: 역할(비로그인·수강생·교수·강사·관리자) × 모든 화면 URL(GET·POST)
 *  2. 정적 리소스·로그인·오류 화면은 로그인 없이 열린다
 *  3. CSRF: 권한이 있어도 토큰이 없거나 틀리면 403 (H2 콘솔만 local 에서 제외)
 *  4. 이중 잠금: 관리용 Service 메서드를 직접 불러도 @PreAuthorize 가 막는다
 *  5. 비밀번호 변경 강제: 변경 전에는 어떤 URL 을 직접 입력해도 /account/password 로 간다
 *  6. 객체 단위: 본인 비밀번호만 변경, 관리자 본인 비활성화 불가, 비활성 계정 로그인 불가,
 *     비활성화·비밀번호 초기화 시 기존 세션 끊김
 *  7. 경로 변형(../, 인코딩된 /, ;, 대소문자, 끝 슬래시)으로 우회되지 않는다
 *
 * 이 클래스는 local 프로필이라 H2 콘솔이 열려 있다. local 이 아닌 환경은 H2ConsoleNonLocalTest 에서 본다.
 */
@IntegrationTest
@Transactional
class AccessControlTest {

    enum Who { ANONYMOUS, STUDENT, PROFESSOR, INSTRUCTOR, ADMIN }

    enum Expect {
        /** 보안 필터를 통과해 컨트롤러까지 간다 (200, 폼 오류 200, 처리 후 리다이렉트, 없는 경로 404 등) */
        PASS,
        /** 로그인 화면으로 */
        LOGIN,
        /** 403 */
        FORBIDDEN
    }

    record Endpoint(HttpMethod method, String url, Set<Who> allowed) {
        @Override
        public String toString() {
            return method + " " + url;
        }
    }

    private static final Set<Who> LOGGED_IN = EnumSet.of(Who.STUDENT, Who.PROFESSOR, Who.INSTRUCTOR, Who.ADMIN);
    private static final Set<Who> NOTICE_WRITERS = EnumSet.of(Who.ADMIN, Who.INSTRUCTOR);
    private static final Set<Who> ADMIN_ONLY = EnumSet.of(Who.ADMIN);

    private static final String CHANGE_URL = PasswordChangeRequiredFilter.CHANGE_URL;
    private static final HttpMethod GET = HttpMethod.GET;
    private static final HttpMethod POST = HttpMethod.POST;

    /** 접근 권한 표. {lessonId}·{noticeId}·{memberId} 는 시드 데이터 id 로 바꿔서 호출한다. */
    private static final List<Endpoint> ENDPOINTS = List.of(
            new Endpoint(GET, "/", LOGGED_IN),
            new Endpoint(GET, "/?month=2026-11&date=2026-11-26", LOGGED_IN),
            new Endpoint(GET, "/lessons/2026-11-02", LOGGED_IN),
            new Endpoint(GET, "/notices", LOGGED_IN),
            new Endpoint(GET, "/notices?category=ATTENDANCE", LOGGED_IN),
            new Endpoint(GET, "/notices/{noticeId}", LOGGED_IN),
            new Endpoint(GET, "/coming-soon?feature=chat", LOGGED_IN),
            new Endpoint(GET, "/account/password", LOGGED_IN),
            new Endpoint(POST, "/account/password", LOGGED_IN),
            // 공지 작성: ADMIN, INSTRUCTOR
            new Endpoint(GET, "/admin/notices/new", NOTICE_WRITERS),
            new Endpoint(POST, "/admin/notices/new", NOTICE_WRITERS),
            // 수업 관리: ADMIN
            new Endpoint(GET, "/admin/lessons", ADMIN_ONLY),
            new Endpoint(GET, "/admin/lessons/new", ADMIN_ONLY),
            new Endpoint(POST, "/admin/lessons/new", ADMIN_ONLY),
            new Endpoint(GET, "/admin/lessons/{lessonId}/edit", ADMIN_ONLY),
            new Endpoint(POST, "/admin/lessons/{lessonId}/edit", ADMIN_ONLY),
            new Endpoint(POST, "/admin/lessons/{lessonId}/resources", ADMIN_ONLY),
            // 회원 관리: ADMIN
            new Endpoint(GET, "/admin/members", ADMIN_ONLY),
            new Endpoint(GET, "/admin/members/new", ADMIN_ONLY),
            new Endpoint(POST, "/admin/members/new", ADMIN_ONLY),
            new Endpoint(POST, "/admin/members/{memberId}/reset-password", ADMIN_ONLY),
            new Endpoint(POST, "/admin/members/{memberId}/deactivate", ADMIN_ONLY),
            // /admin/** 아래 아직 없는 경로도 관리자 외에는 403 (존재 여부를 알려주지 않는다)
            new Endpoint(GET, "/admin/unknown", ADMIN_ONLY),
            // H2 콘솔 (local 전용 체인): ADMIN
            new Endpoint(GET, "/h2-console/", ADMIN_ONLY)
    );

    @Autowired MockMvc mockMvc;
    @Autowired ApplicationContext context;
    @Autowired LoginUserDetailsService loginUserDetailsService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MemberRepository memberRepository;
    @Autowired LessonRepository lessonRepository;
    @Autowired NoticeRepository noticeRepository;
    @Autowired MemberService memberService;
    @Autowired LessonService lessonService;
    @Autowired NoticeService noticeService;

    /** 시드에 교수 계정이 없으므로 테스트마다 만든다 (트랜잭션 롤백으로 사라진다) */
    @BeforeEach
    void createProfessor() {
        memberRepository.save(Member.create("p3-01", passwordEncoder.encode("1234"), "박교수", Role.PROFESSOR, false));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ================================================================
    // 1. URL 접근 권한 표
    // ================================================================

    static Stream<Arguments> accessMatrix() {
        return ENDPOINTS.stream().flatMap(endpoint -> Arrays.stream(Who.values())
                .map(who -> Arguments.of(who, endpoint, expected(endpoint, who))));
    }

    private static Expect expected(Endpoint endpoint, Who who) {
        if (who == Who.ANONYMOUS) {
            return Expect.LOGIN;
        }
        return endpoint.allowed().contains(who) ? Expect.PASS : Expect.FORBIDDEN;
    }

    @ParameterizedTest(name = "{0} → {1} : {2}")
    @MethodSource("accessMatrix")
    void URL_접근_권한_표(Who who, Endpoint endpoint, Expect expect) throws Exception {
        MockHttpServletRequestBuilder request = request(endpoint.method(), resolve(endpoint.url()));
        if (endpoint.method() == POST) {
            request.with(csrf());
        }
        if (who != Who.ANONYMOUS) {
            request.with(user(principalOf(who)));
        }

        MvcResult result = mockMvc.perform(request).andReturn();
        int status = result.getResponse().getStatus();
        String redirect = result.getResponse().getRedirectedUrl();

        switch (expect) {
            case LOGIN -> {
                assertThat(status).as("비로그인은 로그인 화면으로").isEqualTo(302);
                assertThat(redirect).isEqualTo("/login");
            }
            case FORBIDDEN -> assertThat(status).as("권한 없는 역할은 403").isEqualTo(403);
            case PASS -> {
                assertThat(status).as("권한 있는 역할은 보안 필터를 통과").isNotIn(401, 403);
                assertThat(redirect).isNotIn("/login", CHANGE_URL);
            }
        }
    }

    // ================================================================
    // 2. 로그인 없이 열리는 경로
    // ================================================================

    @ParameterizedTest
    @ValueSource(strings = {"/css/app.css", "/css/tokens.css"})
    void 정적_리소스는_로그인_없이_열린다(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    /** 아직 파일은 없지만 허용 경로이므로 로그인 화면이 아니라 404 가 나와야 한다 */
    @ParameterizedTest
    @ValueSource(strings = {"/js/none.js", "/images/none.png", "/favicon.ico"})
    void js_images_favicon_경로도_로그인_없이_허용된다(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void 로그인_화면과_오류_화면은_로그인_없이_열린다() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    // ================================================================
    // 3. CSRF
    // ================================================================

    static Stream<Endpoint> adminPostEndpoints() {
        return ENDPOINTS.stream().filter(e -> e.method() == POST && e.url().startsWith("/admin/"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminPostEndpoints")
    void 관리자라도_CSRF_토큰이_없으면_403(Endpoint endpoint) throws Exception {
        mockMvc.perform(post(resolve(endpoint.url())).with(user(principalOf(Who.ADMIN))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminPostEndpoints")
    void 관리자라도_CSRF_토큰이_틀리면_403(Endpoint endpoint) throws Exception {
        mockMvc.perform(post(resolve(endpoint.url())).with(user(principalOf(Who.ADMIN))).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void 로그인과_비밀번호_변경도_CSRF_토큰이_필요하다() throws Exception {
        mockMvc.perform(post("/login").param("loginId", "s3-001").param("password", "1234"))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
        mockMvc.perform(post("/account/password").with(user(principalOf(Who.STUDENT)))
                        .param("currentPassword", "1234")
                        .param("newPassword", "react2026")
                        .param("newPasswordConfirm", "react2026"))
                .andExpect(status().isForbidden());
        assertThat(passwordEncoder.matches("1234", memberOf("s3-001").getPassword())).isTrue();
    }

    @Test
    void local_에서는_H2_콘솔_경로만_CSRF_검사에서_빠진다() throws Exception {
        assertThat(context.containsBean("h2ConsoleFilterChain")).isTrue();
        assertThat(hasH2ConsoleServlet()).isTrue();

        // H2 콘솔: 토큰 없이도 보안 필터 통과 (MockMvc 에는 콘솔 서블릿이 없어서 404)
        mockMvc.perform(post("/h2-console/login.do").with(user(principalOf(Who.ADMIN))))
                .andExpect(status().isNotFound());
        // 그래도 관리자 외에는 막힌다
        mockMvc.perform(post("/h2-console/login.do").with(user(principalOf(Who.STUDENT))))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // 4. 이중 잠금 — Service 메서드 직접 호출
    // ================================================================

    @ParameterizedTest
    @ValueSource(strings = {"s3-001", "p3-01"})
    void 수강생과_교수는_관리용_Service_를_직접_불러도_막힌다(String loginId) {
        loginAs(loginId);
        assertAllAdminOperationsDenied();
        assertThatThrownBy(() -> noticeService.write(memberOf(loginId).getId(), NoticeCategory.QNA, "제목", "내용", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void 강사는_공지_작성만_되고_나머지_관리용_Service_는_막힌다() {
        loginAs("t3-01");
        assertAllAdminOperationsDenied();
        assertThatCode(() -> noticeService.write(memberOf("t3-01").getId(), NoticeCategory.QNA, "제목", "내용", false))
                .doesNotThrowAnyException();
    }

    @Test
    void 로그인하지_않은_상태로_관리용_Service_를_부르면_막힌다() {
        Long memberId = memberOf("s3-002").getId();
        assertThatThrownBy(() -> memberService.resetPassword(memberId))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> lessonService.register(LocalDate.of(2026, 12, 1), "새 수업", null, null, false))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> noticeService.write(memberId, NoticeCategory.QNA, "제목", "내용", false))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void 관리자는_관리용_Service_를_모두_쓸_수_있다() {
        loginAs(IntegrationTest.ADMIN_ID);
        Long lessonId = lessonId();
        Long memberId = memberOf("s3-002").getId();
        Long adminId = memberOf(IntegrationTest.ADMIN_ID).getId();

        assertThatCode(() -> {
            memberService.issue("s3-201", "홍길동", Role.STUDENT);
            memberService.resetPassword(memberId);
            memberService.deactivate(memberId, adminId);
            lessonService.register(LocalDate.of(2026, 12, 1), "새 수업", null, null, false);
            lessonService.update(lessonId, "HTML 기초", null, null, false);
            lessonService.addResource(lessonId, ResourceType.ARTIFACT, "자료", "https://example.com/a");
            noticeService.write(adminId, NoticeCategory.QNA, "제목", "내용", false);
        }).doesNotThrowAnyException();
    }

    private void assertAllAdminOperationsDenied() {
        Long lessonId = lessonId();
        Long memberId = memberOf("s3-002").getId();
        List<Runnable> operations = List.of(
                () -> memberService.issue("s3-200", "홍길동", Role.ADMIN),
                () -> memberService.resetPassword(memberId),
                () -> memberService.deactivate(memberId, -1L),
                () -> lessonService.register(LocalDate.of(2026, 12, 1), "새 수업", null, null, false),
                () -> lessonService.update(lessonId, "바뀐 제목", null, null, true),
                () -> lessonService.addResource(lessonId, ResourceType.ARTIFACT, "자료", "https://example.com/a"));
        operations.forEach(operation ->
                assertThatThrownBy(operation::run).isInstanceOf(AccessDeniedException.class));

        assertThat(memberRepository.existsByLoginId("s3-200")).isFalse();
        assertThat(memberOf("s3-002").isActive()).isTrue();
        assertThat(lessonRepository.existsByLessonDate(LocalDate.of(2026, 12, 1))).isFalse();
    }

    // ================================================================
    // 5. 비밀번호 변경 강제 — URL 을 직접 입력해도 우회되지 않는다
    // ================================================================

    static Stream<Arguments> mustChangeCases() {
        return Stream.of(Who.STUDENT, Who.ADMIN).flatMap(who -> ENDPOINTS.stream()
                .filter(e -> !e.url().equals(CHANGE_URL))
                .map(e -> Arguments.of(who, e)));
    }

    /** 관리자라도 변경 전에는 관리자 화면에 못 들어가고, 수강생도 403 이 아니라 변경 화면으로 간다 */
    @ParameterizedTest(name = "변경 전 {0} → {1}")
    @MethodSource("mustChangeCases")
    void 비밀번호를_바꾸기_전에는_어떤_URL_을_직접_입력해도_변경_화면으로_간다(Who who, Endpoint endpoint) throws Exception {
        LoginUser loginUser = mustChangeUser(loginIdOf(who));
        MockHttpServletRequestBuilder request = request(endpoint.method(), resolve(endpoint.url())).with(user(loginUser));
        if (endpoint.method() == POST) {
            request.with(csrf());
        }
        mockMvc.perform(request).andExpect(redirectedUrl(CHANGE_URL));
    }

    @Test
    void 비밀번호를_바꾸기_전에도_변경_화면_정적_리소스_로그아웃은_쓸_수_있다() throws Exception {
        LoginUser loginUser = mustChangeUser("s3-001");
        mockMvc.perform(get(CHANGE_URL).with(user(loginUser))).andExpect(status().isOk());
        mockMvc.perform(get("/css/app.css").with(user(loginUser))).andExpect(status().isOk());
        mockMvc.perform(get("/favicon.ico").with(user(loginUser))).andExpect(status().isNotFound());
        mockMvc.perform(post("/logout").with(user(loginUser)).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/account/password/", "/Account/password", "/account/password-x", "/css-fake/x"})
    void 변경_화면과_비슷한_경로로는_통과되지_않는다(String url) throws Exception {
        mockMvc.perform(get(url).with(user(mustChangeUser("s3-001"))))
                .andExpect(redirectedUrl(CHANGE_URL));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/css/../admin/members",
            "/account/password/../../admin/members",
            "/account/password;x=1",
            "/css/%2e%2e/admin/members"})
    void 허용_경로에_경로_조작을_섞으면_요청_자체가_거부된다(String url) throws Exception {
        mockMvc.perform(get(URI.create(url)).with(user(mustChangeUser("s3-001"))))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // 6. 객체 단위 검사
    // ================================================================

    @Test
    void 비밀번호_변경은_로그인한_본인_계정에만_적용된다_다른_회원_id_파라미터는_무시된다() throws Exception {
        Member other = memberOf("s3-002");

        mockMvc.perform(post("/account/password").with(user(principalOf(Who.STUDENT))).with(csrf())
                        .param("memberId", other.getId().toString())
                        .param("id", other.getId().toString())
                        .param("loginId", "s3-002")
                        .param("currentPassword", "1234")
                        .param("newPassword", "react2026")
                        .param("newPasswordConfirm", "react2026"))
                .andExpect(redirectedUrl("/"));

        assertThat(passwordEncoder.matches("react2026", memberOf("s3-001").getPassword())).isTrue();
        assertThat(passwordEncoder.matches("1234", memberOf("s3-002").getPassword())).isTrue();
    }

    @Test
    void 비밀번호_변경_Service_도_다른_회원_id_로는_부를_수_없다() {
        loginAs("s3-001");
        Long otherId = memberOf("s3-002").getId();

        assertThatThrownBy(() -> memberService.changePassword(otherId, "1234", "react2026"))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(passwordEncoder.matches("1234", memberOf("s3-002").getPassword())).isTrue();

        assertThatCode(() -> memberService.changePassword(memberOf("s3-001").getId(), "1234", "react2026"))
                .doesNotThrowAnyException();
    }

    @Test
    void 관리자는_자기_자신을_비활성화할_수_없다() throws Exception {
        Long selfId = memberOf(IntegrationTest.ADMIN_ID).getId();

        mockMvc.perform(post("/admin/members/{id}/deactivate", selfId).with(user(principalOf(Who.ADMIN))).with(csrf()))
                .andExpect(redirectedUrl("/admin/members"))
                .andExpect(flash().attribute("flashError", "본인 계정은 비활성화할 수 없어요."));

        loginAs(IntegrationTest.ADMIN_ID);
        assertThatThrownBy(() -> memberService.deactivate(selfId, selfId)).isInstanceOf(BusinessException.class);
        assertThat(memberOf(IntegrationTest.ADMIN_ID).isActive()).isTrue();
    }

    @Test
    void 비활성화된_계정은_로그인할_수_없다() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-003").password("1234"))
                .andExpect(authenticated());

        memberOf("s3-003").deactivate();

        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-003").password("1234"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void 비활성화하면_이미_로그인해_있던_세션도_끊긴다() throws Exception {
        MockHttpSession target = login("s3-003", "1234");
        MockHttpSession targetSecondDevice = login("s3-003", "1234");
        MockHttpSession bystander = login("s3-002", "1234");
        mockMvc.perform(get("/").session(target)).andExpect(status().isOk());

        mockMvc.perform(post("/admin/members/{id}/deactivate", memberOf("s3-003").getId())
                        .with(user(principalOf(Who.ADMIN))).with(csrf()))
                .andExpect(flash().attributeExists("flashMessage"));

        // 다음 요청에서 로그아웃되어 로그인 화면으로 (모든 기기의 세션)
        mockMvc.perform(get("/").session(target)).andExpect(redirectedUrl("/login?expired"));
        mockMvc.perform(get("/notices").session(targetSecondDevice)).andExpect(redirectedUrl("/login?expired"));
        assertThat(target.isInvalid()).isTrue();
        // 다른 회원의 세션은 그대로
        mockMvc.perform(get("/").session(bystander)).andExpect(status().isOk());
        // 안내 문구
        mockMvc.perform(get("/login?expired"))
                .andExpect(content().string(containsString("로그인이 해제되었어요. 다시 로그인해 주세요.")));
    }

    @Test
    void 비활성화_화면을_거치지_않고_Service_로_비활성화해도_세션이_끊긴다() throws Exception {
        MockHttpSession target = login("s3-003", "1234");

        loginAs(IntegrationTest.ADMIN_ID);
        memberService.deactivate(memberOf("s3-003").getId(), memberOf(IntegrationTest.ADMIN_ID).getId());

        mockMvc.perform(post("/account/password").session(target).with(csrf())
                        .param("currentPassword", "1234")
                        .param("newPassword", "react2026")
                        .param("newPasswordConfirm", "react2026"))
                .andExpect(redirectedUrl("/login?expired"));
        assertThat(passwordEncoder.matches("1234", memberOf("s3-003").getPassword())).isTrue();
    }

    @Test
    void 비밀번호를_초기화하면_이미_로그인해_있던_세션도_끊기고_새_임시_비밀번호로_변경을_강제한다() throws Exception {
        MockHttpSession target = login("s3-002", "1234");
        MockHttpSession bystander = login("s3-001", "1234");

        MvcResult reset = mockMvc.perform(post("/admin/members/{id}/reset-password", memberOf("s3-002").getId())
                        .with(user(principalOf(Who.ADMIN))).with(csrf()))
                .andExpect(redirectedUrl("/admin/members"))
                .andReturn();
        IssuedPasswordDto issued = (IssuedPasswordDto) reset.getFlashMap().get("issuedPassword");

        mockMvc.perform(get("/").session(target)).andExpect(redirectedUrl("/login?expired"));
        mockMvc.perform(get("/").session(bystander)).andExpect(status().isOk());
        // 새 임시 비밀번호로 다시 로그인하면 변경 화면으로
        mockMvc.perform(formLogin("/login").userParameter("loginId").user("s3-002").password(issued.password()))
                .andExpect(redirectedUrl(CHANGE_URL));
    }

    /** 지금 세션까지 끊으면 한 번만 보여주는 임시 비밀번호를 못 보고 로그아웃된다 */
    @Test
    void 관리자가_자기_비밀번호를_초기화하면_지금_세션은_남고_다른_기기_세션만_끊긴다() throws Exception {
        MockHttpSession current = login(IntegrationTest.ADMIN_ID, IntegrationTest.ADMIN_PASSWORD);
        MockHttpSession otherDevice = login(IntegrationTest.ADMIN_ID, IntegrationTest.ADMIN_PASSWORD);

        MvcResult reset = mockMvc.perform(post("/admin/members/{id}/reset-password",
                        memberOf(IntegrationTest.ADMIN_ID).getId()).session(current).with(csrf()))
                .andExpect(redirectedUrl("/admin/members"))
                .andReturn();
        IssuedPasswordDto issued = (IssuedPasswordDto) reset.getFlashMap().get("issuedPassword");

        mockMvc.perform(get("/admin/members").session(current))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(issued.password())));
        mockMvc.perform(get("/admin/members").session(otherDevice)).andExpect(redirectedUrl("/login?expired"));
    }

    // ================================================================
    // 7. 경로 변형으로 관리자 URL 우회
    // ================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/members/",
            "/admin//members",
            "/ADMIN/members",
            "/admin;x=1/members",
            "/admin%2Fmembers",
            "/css/../admin/members",
            "/notices/../admin/members"})
    void 수강생은_경로를_바꿔_써도_관리자_화면에_닿지_못한다(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(URI.create(url)).with(user(principalOf(Who.STUDENT)))).andReturn();
        // 400: 방화벽이 거부, 403: 권한 없음, 404: 대소문자가 다르면 아예 다른(없는) 경로
        assertThat(result.getResponse().getStatus()).isIn(400, 403, 404);
    }

    // ================================================================
    // 도우미
    // ================================================================

    private String resolve(String url) {
        return url
                .replace("{lessonId}", lessonId().toString())
                .replace("{noticeId}", noticeRepository.findAll().getFirst().getId().toString())
                .replace("{memberId}", memberOf("s3-002").getId().toString());
    }

    private Long lessonId() {
        return lessonRepository.findByLessonDate(LocalDate.of(2026, 11, 2)).orElseThrow().getId();
    }

    private Member memberOf(String loginId) {
        return memberRepository.findByLoginId(loginId).orElseThrow();
    }

    private static String loginIdOf(Who who) {
        return switch (who) {
            case STUDENT -> "s3-001";
            case PROFESSOR -> "p3-01";
            case INSTRUCTOR -> "t3-01";
            case ADMIN -> IntegrationTest.ADMIN_ID;
            case ANONYMOUS -> throw new IllegalArgumentException("비로그인은 계정이 없다");
        };
    }

    private LoginUser principalOf(Who who) {
        return (LoginUser) loginUserDetailsService.loadUserByUsername(loginIdOf(who));
    }

    /** 관리자가 비밀번호를 초기화한 상태(mustChangePassword = true)로 로그인한 사용자 */
    private LoginUser mustChangeUser(String loginId) {
        memberOf(loginId).resetPassword(passwordEncoder.encode("Temp12345a"));
        LoginUser loginUser = (LoginUser) loginUserDetailsService.loadUserByUsername(loginId);
        assertThat(loginUser.isMustChangePassword()).isTrue();
        return loginUser;
    }

    /** 실제 폼 로그인으로 세션을 만든다 (SessionRegistry 에 기록된다) */
    private MockHttpSession login(String loginId, String password) throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("loginId").user(loginId).password(password))
                .andExpect(authenticated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /** Service 직접 호출용 로그인 (MockMvc 요청과는 상관없다) */
    private void loginAs(String loginId) {
        LoginUser loginUser = (LoginUser) loginUserDetailsService.loadUserByUsername(loginId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(loginUser, null, loginUser.getAuthorities()));
    }

    private boolean hasH2ConsoleServlet() {
        return H2ConsoleNonLocalTest.hasH2ConsoleServlet(context);
    }
}
