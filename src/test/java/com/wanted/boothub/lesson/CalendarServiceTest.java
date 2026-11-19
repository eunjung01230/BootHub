package com.wanted.boothub.lesson;

import com.wanted.boothub.lesson.dto.CalendarDayDto;
import com.wanted.boothub.lesson.dto.CalendarMonthDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class CalendarServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(ZonedDateTime.of(2026, 11, 19, 10, 0, 0, 0, SEOUL).toInstant(), SEOUL);
        calendarService = new CalendarService(lessonRepository, fixed);

        Lesson withMaterial = Lesson.create(LocalDate.of(2026, 11, 2), "HTML 기초", null, null, false);
        withMaterial.addResource(ResourceType.ARTIFACT, "자료", "https://example.com");
        Lesson important = Lesson.create(LocalDate.of(2026, 11, 27), "결과 발표", null, null, true);
        given(lessonRepository.findByLessonDateBetweenOrderByLessonDateAsc(any(), any()))
                .willReturn(List.of(withMaterial, important));
    }

    @Test
    void 일요일부터_시작하는_7칸_주_단위_격자를_만든다() {
        CalendarMonthDto month = calendarService.getMonth(YearMonth.of(2026, 11), null);

        assertThat(month.label()).isEqualTo("2026년 11월");
        assertThat(month.prevMonth()).isEqualTo("2026-10");
        assertThat(month.nextMonth()).isEqualTo("2026-12");
        assertThat(month.weeks()).hasSize(5).allSatisfy(week -> assertThat(week).hasSize(7));

        // 2026-11-01 은 일요일 → 첫 칸이 1일
        CalendarDayDto first = month.weeks().get(0).get(0);
        assertThat(first.inMonth()).isTrue();
        assertThat(first.day()).isEqualTo(1);
        assertThat(first.sunday()).isTrue();

        // 30일(월) 뒤 화~토는 빈 칸
        List<CalendarDayDto> lastWeek = month.weeks().get(4);
        assertThat(lastWeek.get(1).day()).isEqualTo(30);
        assertThat(lastWeek.subList(2, 7)).noneMatch(CalendarDayDto::inMonth);
    }

    @Test
    void 수업_자료와_중요_일정을_칸에_표시한다() {
        CalendarMonthDto month = calendarService.getMonth(YearMonth.of(2026, 11), null);

        CalendarDayDto day2 = month.weeks().get(0).get(1);
        assertThat(day2.lessonTitle()).isEqualTo("HTML 기초");
        assertThat(day2.materialReady()).isTrue();
        assertThat(day2.important()).isFalse();

        CalendarDayDto day27 = month.weeks().get(3).get(5);
        assertThat(day27.day()).isEqualTo(27);
        assertThat(day27.important()).isTrue();
        assertThat(day27.materialReady()).isFalse();
    }

    @Test
    void 날짜를_고르지_않으면_이번_달은_오늘을_선택한다() {
        CalendarMonthDto month = calendarService.getMonth(null, null);

        assertThat(month.month()).isEqualTo("2026-11");
        assertThat(month.selectedDate()).isEqualTo(LocalDate.of(2026, 11, 19));
    }

    @Test
    void 다른_달은_1일을_선택하고_날짜만_주면_그_달을_보여준다() {
        assertThat(calendarService.getMonth(YearMonth.of(2026, 12), null).selectedDate())
                .isEqualTo(LocalDate.of(2026, 12, 1));

        CalendarMonthDto byDate = calendarService.getMonth(null, LocalDate.of(2027, 1, 5));
        assertThat(byDate.month()).isEqualTo("2027-01");
        assertThat(byDate.selectedDate()).isEqualTo(LocalDate.of(2027, 1, 5));
    }

    @Test
    void 선택_날짜가_보여주는_달_밖이면_무시한다() {
        // 11월을 보면서 12월 5일을 고르면: 11월은 이번 달이므로 오늘(19일)을 선택
        CalendarMonthDto month = calendarService.getMonth(YearMonth.of(2026, 11), LocalDate.of(2026, 12, 5));

        assertThat(month.month()).isEqualTo("2026-11");
        assertThat(month.selectedDate()).isEqualTo(LocalDate.of(2026, 11, 19));
    }

    @Test
    void 형식이_틀린_쿼리_값은_없는_것으로_보고_이번_달을_보여준다() {
        assertThat(calendarService.getMonthFromQuery("abc", "2026-02-30").month()).isEqualTo("2026-11");
        assertThat(calendarService.getMonthFromQuery("2026-13", null).month()).isEqualTo("2026-11");
        assertThat(calendarService.getMonthFromQuery("2026-12", "nope").selectedDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }
}
