package com.wanted.boothub.notice;

import com.wanted.boothub.member.LoginUserDetailsService;
import com.wanted.boothub.notice.dto.NoticeSummaryDto;
import com.wanted.boothub.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class NoticeControllerTest {

    private static final String PINNED_TITLE = "프로젝트 평가 및 제출 안내";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LoginUserDetailsService loginUserDetailsService;

    @SuppressWarnings("unchecked")
    private List<NoticeSummaryDto> noticesOf(MvcResult result) {
        return (List<NoticeSummaryDto>) result.getModelAndView().getModel().get("notices");
    }

    private UserDetails loginAs(String loginId) {
        return loginUserDetailsService.loadUserByUsername(loginId);
    }

    @Test
    @WithMockUser
    void 전체_공지는_필독이_맨_위에_온다() throws Exception {
        MvcResult result = mockMvc.perform(get("/notices"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1 class=\"page-title\">전체 공지</h1>")))
                .andReturn();

        List<NoticeSummaryDto> notices = noticesOf(result);
        assertThat(notices).hasSizeGreaterThanOrEqualTo(5);
        assertThat(notices.getFirst().title()).isEqualTo(PINNED_TITLE);
        assertThat(notices.getFirst().pinned()).isTrue();
        assertThat(notices.getFirst().authorName()).isEqualTo("관리자");
    }

    /** 사이드바 공지 메뉴 5개 (카테고리) 가 각각 동작한다 */
    @ParameterizedTest
    @EnumSource(NoticeCategory.class)
    @WithMockUser
    void 카테고리_메뉴는_해당_카테고리_공지만_보여주고_사이드바에_표시된다(NoticeCategory category) throws Exception {
        MvcResult result = mockMvc.perform(get("/notices").param("category", category.name()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "class=\"nav-sub is-active\" href=\"/notices?category=" + category.name() + "\"")))
                .andReturn();

        assertThat(noticesOf(result)).isNotEmpty()
                .allSatisfy(notice -> assertThat(notice.categoryLabel()).isEqualTo(category.getLabel()));
    }

    @Test
    @WithMockUser
    void 모르는_카테고리는_전체_공지로_보여준다() throws Exception {
        mockMvc.perform(get("/notices").param("category", "nope"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedCategory", (Object) null))
                .andExpect(content().string(containsString(PINNED_TITLE)));
    }

    @Test
    @WithMockUser
    void 공지_상세와_없는_공지() throws Exception {
        MvcResult list = mockMvc.perform(get("/notices").param("category", "EVALUATION")).andReturn();
        Long id = noticesOf(list).getFirst().id();

        mockMvc.perform(get("/notices/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(PINNED_TITLE)))
                .andExpect(content().string(containsString("← 평가 관련 목록으로")));
        mockMvc.perform(get("/notices/999999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/notices/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void 홈에_필독_공지_배너가_보인다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"notice-banner\"")))
                .andExpect(content().string(containsString(PINNED_TITLE)));
    }

    @Test
    void 수강생에게는_작성_버튼이_없고_작성_화면도_막힌다() throws Exception {
        mockMvc.perform(get("/notices").with(user(loginAs("student1"))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("공지 작성"))));
        mockMvc.perform(get("/admin/notices/new").with(user(loginAs("student1"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/notices/new").with(user(loginAs("student1"))).with(csrf())
                        .param("category", "QNA").param("title", "t").param("content", "c"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 강사는_보던_카테고리가_선택된_작성_화면을_연다() throws Exception {
        mockMvc.perform(get("/notices").param("category", "QNA").with(user(loginAs("instructor"))))
                .andExpect(content().string(containsString("href=\"/admin/notices/new?category=QNA\"")));
        mockMvc.perform(get("/admin/notices/new").param("category", "QNA").with(user(loginAs("instructor"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"QNA\" selected=\"selected\"")));
    }

    @Test
    @Transactional
    void 강사가_작성한_공지는_실명과_함께_목록과_상세에_보인다() throws Exception {
        UserDetails instructor = loginAs("instructor");
        MvcResult created = mockMvc.perform(post("/admin/notices/new").with(user(instructor)).with(csrf())
                        .param("category", "QNA")
                        .param("title", "  과제 제출 질문 정리  ")
                        .param("content", "첫 줄\n<script>alert(1)</script>"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/notices/*"))
                .andReturn();

        mockMvc.perform(get(created.getResponse().getRedirectedUrl()).with(user(instructor)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1 class=\"page-title\">과제 제출 질문 정리</h1>")))
                .andExpect(content().string(containsString("이서연")))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));

        mockMvc.perform(get("/notices").param("category", "QNA").with(user(instructor)))
                .andExpect(content().string(containsString("과제 제출 질문 정리")));
    }

    @Test
    void 필수값이_비면_작성_화면에_오류를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/notices/new").with(user(loginAs(IntegrationTest.ADMIN_ID))).with(csrf())
                        .param("category", "").param("title", " ").param("content", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("카테고리를 골라 주세요.")))
                .andExpect(content().string(containsString("제목을 입력해 주세요.")))
                .andExpect(content().string(containsString("내용을 입력해 주세요.")));
    }
}
