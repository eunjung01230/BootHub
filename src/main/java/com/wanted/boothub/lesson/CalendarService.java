package com.wanted.boothub.lesson;

import com.wanted.boothub.lesson.dto.CalendarDayDto;
import com.wanted.boothub.lesson.dto.CalendarMonthDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 월간 캘린더 데이터 생성. 일요일 시작, 주 단위 7칸 격자.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy년 M월");
    private static final DateTimeFormatter MONTH_PARAM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LessonRepository lessonRepository;
    private final Clock clock;

    /**
     * @param month        보여줄 달 (없으면 date 의 달, 그것도 없으면 이번 달)
     * @param selectedDate 선택 날짜 (없거나 보여주는 달 밖이면: 이번 달이면 오늘, 아니면 그 달 1일)
     */
    public CalendarMonthDto getMonth(YearMonth month, LocalDate selectedDate) {
        LocalDate today = LocalDate.now(clock);
        YearMonth target = resolveMonth(month, selectedDate, today);
        LocalDate selected = resolveSelected(target, selectedDate, today);

        Map<LocalDate, Lesson> lessons = lessonRepository
                .findByLessonDateBetweenOrderByLessonDateAsc(target.atDay(1), target.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(Lesson::getLessonDate, Function.identity()));

        return new CalendarMonthDto(
                target.format(LABEL),
                target.format(MONTH_PARAM),
                target.minusMonths(1).format(MONTH_PARAM),
                target.plusMonths(1).format(MONTH_PARAM),
                selected,
                buildWeeks(target, selected, lessons)
        );
    }

    private YearMonth resolveMonth(YearMonth month, LocalDate selectedDate, LocalDate today) {
        if (month != null) {
            return month;
        }
        return selectedDate != null ? YearMonth.from(selectedDate) : YearMonth.from(today);
    }

    /** 선택 날짜가 보여주는 달 밖이면 무시한다 (캘린더와 상세 패널이 항상 같은 달을 보도록) */
    private LocalDate resolveSelected(YearMonth target, LocalDate selectedDate, LocalDate today) {
        if (selectedDate != null && YearMonth.from(selectedDate).equals(target)) {
            return selectedDate;
        }
        return YearMonth.from(today).equals(target) ? today : target.atDay(1);
    }

    /**
     * 쿼리 문자열 그대로 받는 버전. 형식이 틀린 값(?month=abc, ?date=2026-02-30)은 없는 것으로 보고
     * 기본값(이번 달·오늘)으로 보여준다.
     */
    public CalendarMonthDto getMonthFromQuery(String month, String selectedDate) {
        return getMonth(parseMonth(month), parseDate(selectedDate));
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value, MONTH_PARAM);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<List<CalendarDayDto>> buildWeeks(YearMonth month, LocalDate selected, Map<LocalDate, Lesson> lessons) {
        LocalDate start = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        List<List<CalendarDayDto>> weeks = new ArrayList<>();
        List<CalendarDayDto> week = new ArrayList<>(7);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            week.add(YearMonth.from(date).equals(month)
                    ? toDay(date, selected, lessons.get(date))
                    : CalendarDayDto.blank(date));
            if (week.size() == 7) {
                weeks.add(List.copyOf(week));
                week.clear();
            }
        }
        return List.copyOf(weeks);
    }

    private CalendarDayDto toDay(LocalDate date, LocalDate selected, Lesson lesson) {
        return new CalendarDayDto(
                date,
                date.getDayOfMonth(),
                true,
                date.getDayOfWeek() == DayOfWeek.SUNDAY,
                date.equals(selected),
                lesson != null ? lesson.getTitle() : null,
                lesson != null && lesson.isMaterialReady(),
                lesson != null && lesson.isImportant()
        );
    }
}
