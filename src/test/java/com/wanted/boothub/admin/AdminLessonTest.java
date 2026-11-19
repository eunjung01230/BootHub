package com.wanted.boothub.admin;

import com.wanted.boothub.lesson.Lesson;
import com.wanted.boothub.lesson.LessonRepository;
import com.wanted.boothub.lesson.dto.LessonDetailDto;
import com.wanted.boothub.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 5단계 수업 관리: 등록·수정·자료 링크 추가 → 홈 캘린더 반영.
 */
@IntegrationTest
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminLessonTest {

    private static final String URL_ERROR = "http:// 또는 https:// 로 시작하는 주소를 넣어 주세요.";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LessonRepository lessonRepository;

    private LessonDetailDto homePanel(String date) throws Exception {
        MvcResult result = mockMvc.perform(get("/").param("date", date)).andReturn();
        return (LessonDetailDto) result.getModelAndView().getModel().get("lesson");
    }

    @Test
    void 관리자가_등록한_수업과_자료_링크가_홈_캘린더에_반영된다() throws Exception {
        MvcResult created = mockMvc.perform(post("/admin/lessons/new").with(csrf())
                        .param("lessonDate", "2026-12-07")
                        .param("title", "  Spring 입문  ")
                        .param("description", "")
                        .param("githubUrl", "")
                        .param("important", "true"))
                .andExpect(redirectedUrlPattern("/admin/lessons/*/edit"))
                .andReturn();
        String editUrl = created.getResponse().getRedirectedUrl();

        // 캘린더에 수업명이 보이고, 자료가 없으니 아직 초록 점은 없다
        mockMvc.perform(get("/").param("month", "2026-12"))
                .andExpect(content().string(containsString("Spring 입문")));
        LessonDetailDto before = homePanel("2026-12-07");
        assertThat(before.title()).isEqualTo("Spring 입문");
        assertThat(before.important()).isTrue();
        assertThat(before.materialReady()).isFalse();
        assertThat(before.description()).isNull();

        // 자료 링크 추가 → 초록 점, 강사 아티팩트 버튼 활성
        String resourcesUrl = editUrl.replace("/edit", "/resources");
        mockMvc.perform(post(resourcesUrl).with(csrf())
                        .param("type", "ARTIFACT")
                        .param("title", "Spring 정리 자료")
                        .param("url", "https://example.com/spring"))
                .andExpect(redirectedUrl(editUrl));

        LessonDetailDto after = homePanel("2026-12-07");
        assertThat(after.materialReady()).isTrue();
        assertThat(after.artifactUrl()).isEqualTo("https://example.com/spring");

        String editPage = mockMvc.perform(get(editUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Spring 정리 자료")))
                .andReturn().getResponse().getContentAsString();
        // 수업 폼과 자료 폼이 한 화면에 있어도 입력칸 id 가 겹치지 않는다
        assertThat(editPage.split("id=\"title\"", -1)).hasSize(2);
        assertThat(editPage).contains("id=\"resourceTitle\"", "id=\"resourceUrl\"");
        mockMvc.perform(get("/admin/lessons"))
                .andExpect(content().string(containsString("Spring 입문")));
    }

    @Test
    void 수업을_수정해도_날짜는_바뀌지_않는다() throws Exception {
        Lesson lesson = lessonRepository.findByLessonDate(LocalDate.of(2026, 11, 20)).orElseThrow();

        mockMvc.perform(get("/admin/lessons/{id}/edit", lesson.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"API 연동\"")));

        mockMvc.perform(post("/admin/lessons/{id}/edit", lesson.getId()).with(csrf())
                        .param("lessonDate", "2026-12-25")
                        .param("title", "API 연동 실습")
                        .param("githubUrl", "https://github.com/example/api"))
                .andExpect(redirectedUrl("/admin/lessons/" + lesson.getId() + "/edit"));

        LessonDetailDto panel = homePanel("2026-11-20");
        assertThat(panel.title()).isEqualTo("API 연동 실습");
        assertThat(panel.githubUrl()).isEqualTo("https://github.com/example/api");
        assertThat(lessonRepository.findByLessonDate(LocalDate.of(2026, 12, 25))).isEmpty();
    }

    @Test
    void 이미_수업이_있는_날짜에는_등록할_수_없다() throws Exception {
        mockMvc.perform(post("/admin/lessons/new").with(csrf())
                        .param("lessonDate", "2026-11-02").param("title", "중복 수업"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 수업이 있는 날짜예요.")));
    }

    @Test
    void http_주소가_아닌_링크는_받지_않는다() throws Exception {
        mockMvc.perform(post("/admin/lessons/new").with(csrf())
                        .param("lessonDate", "2026-12-08").param("title", "링크 검사")
                        .param("githubUrl", "javascript:alert(1)"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(URL_ERROR)));

        Lesson lesson = lessonRepository.findByLessonDate(LocalDate.of(2026, 11, 20)).orElseThrow();
        mockMvc.perform(post("/admin/lessons/{id}/resources", lesson.getId()).with(csrf())
                        .param("type", "ARTIFACT").param("title", "자료").param("url", "javascript:alert(1)"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(URL_ERROR)));
        assertThat(homePanel("2026-11-20").materialReady()).isFalse();
    }

    @Test
    void 필수값이_비면_등록_화면에_오류를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/lessons/new").with(csrf()).param("lessonDate", "").param("title", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("날짜를 골라 주세요.")))
                .andExpect(content().string(containsString("수업명을 입력해 주세요.")));
    }

    @Test
    void 없는_수업은_404() throws Exception {
        mockMvc.perform(get("/admin/lessons/999999/edit")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void 수강생은_수업을_등록할_수_없다() throws Exception {
        mockMvc.perform(post("/admin/lessons/new").with(csrf())
                        .param("lessonDate", "2026-12-09").param("title", "몰래 등록"))
                .andExpect(status().isForbidden());
    }
}
