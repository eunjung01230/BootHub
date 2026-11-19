package com.wanted.boothub.home;

import com.wanted.boothub.support.IntegrationTest;
import com.wanted.boothub.lesson.dto.CalendarDayDto;
import com.wanted.boothub.lesson.dto.CalendarMonthDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@WithMockUser
class HomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    private CalendarMonthDto calendarOf(MvcResult result) {
        return (CalendarMonthDto) result.getModelAndView().getModel().get("calendar");
    }

    private List<CalendarDayDto> daysOf(CalendarMonthDto calendar) {
        return calendar.weeks().stream().flatMap(List::stream).filter(CalendarDayDto::inMonth).toList();
    }

    private CalendarDayDto dayOf(CalendarMonthDto calendar, int day) {
        return daysOf(calendar).stream().filter(d -> d.day() == day).findFirst().orElseThrow();
    }

    @Test
    void 선택_날짜와_중요_일정이_정확한_칸에_표시된다() throws Exception {
        MvcResult result = mockMvc.perform(get("/").param("month", "2026-11").param("date", "2026-11-26"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2026년 11월")))
                .andExpect(content().string(containsString("11월 26일 (목)")))
                .andReturn();

        CalendarMonthDto calendar = calendarOf(result);
        assertThat(daysOf(calendar).stream().filter(CalendarDayDto::selected))
                .singleElement().extracting(CalendarDayDto::day).isEqualTo(26);
        assertThat(daysOf(calendar).stream().filter(CalendarDayDto::important))
                .singleElement().extracting(CalendarDayDto::day).isEqualTo(27);
        assertThat(dayOf(calendar, 2).materialReady()).isTrue();
        assertThat(dayOf(calendar, 20).materialReady()).isFalse();
    }

    @Test
    void 일요일_숫자와_수업명_title_속성을_그린다() throws Exception {
        mockMvc.perform(get("/").param("month", "2026-11"))
                .andExpect(content().string(containsString("class=\"calendar-day is-sunday\"")))
                .andExpect(content().string(containsString("title=\"State와 Hooks\"")));
    }

    @Test
    void 자료가_있는_수업은_GitHub과_아티팩트_버튼이_열린다() throws Exception {
        mockMvc.perform(get("/").param("date", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("GitHub 코드 ↗")))
                .andExpect(content().string(containsString("강사 아티팩트 ↗")))
                .andExpect(content().string(not(containsString("자료 없음"))));
    }

    @Test
    void 자료가_없는_수업은_GitHub과_아티팩트_버튼이_비활성이다() throws Exception {
        mockMvc.perform(get("/").param("date", "2026-11-24"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("GitHub 코드 ↗"))))
                .andExpect(content().string(not(containsString("강사 아티팩트 ↗"))))
                .andExpect(content().string(containsString("GitHub 코드<span class=\"lesson-action-tag\">자료 없음")))
                .andExpect(content().string(containsString("강사 아티팩트<span class=\"lesson-action-tag\">자료 없음")));
    }

    @Test
    void 수업이_없는_날도_버튼_6개를_비활성으로_보여준다() throws Exception {
        String html = mockMvc.perform(get("/").param("date", "2026-11-22"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이 날은 등록된 수업이 없어요.")))
                .andReturn().getResponse().getContentAsString();

        assertThat(html.split("class=\"lesson-action is-disabled\"", -1)).hasSize(7);
    }

    @Test
    void 준비_중_버튼_4개와_하단_카드_2개는_준비_중_페이지로_연결된다() throws Exception {
        String html = mockMvc.perform(get("/").param("date", "2026-11-02"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(
                "href=\"/coming-soon?feature=ai-summary\"",
                "href=\"/coming-soon?feature=quiz\"",
                "href=\"/coming-soon?feature=mini-project\"",
                "href=\"/coming-soon?feature=lesson-qna\"",
                "class=\"card soon-card\" href=\"/coming-soon?feature=chat\"",
                "class=\"card soon-card\" href=\"/coming-soon?feature=showcase\"");
    }

    @Test
    void 형식이_틀린_쿼리는_이번_달로_보여준다() throws Exception {
        MvcResult result = mockMvc.perform(get("/").param("month", "abc").param("date", "2026-02-30"))
                .andExpect(status().isOk())
                .andReturn();

        String thisMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).toString();
        assertThat(calendarOf(result).month()).isEqualTo(thisMonth);
    }

    @Test
    void 다른_달의_날짜를_고르면_무시한다() throws Exception {
        MvcResult result = mockMvc.perform(get("/").param("month", "2026-12").param("date", "2026-11-26"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(calendarOf(result).selectedDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void 수업_상세_화면() throws Exception {
        mockMvc.perform(get("/lessons/2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HTML 기초")))
                .andExpect(content().string(containsString("HTML 기초 정리 자료")));
    }

    @Test
    void 수업이_없는_날의_상세는_404_형식이_틀리면_400() throws Exception {
        mockMvc.perform(get("/lessons/2026-11-01")).andExpect(status().isNotFound());
        mockMvc.perform(get("/lessons/abc")).andExpect(status().isBadRequest());
    }
}
