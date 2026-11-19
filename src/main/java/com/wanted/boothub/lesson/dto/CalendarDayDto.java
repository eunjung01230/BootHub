package com.wanted.boothub.lesson.dto;

import java.time.LocalDate;

/**
 * 캘린더 격자의 칸 하나. 이번 달이 아닌 칸(inMonth=false)은 빈 칸으로 그린다.
 *
 * @param lessonTitle   수업명 (수업이 없으면 null)
 * @param materialReady 자료 준비 완료 (초록 점)
 * @param important     중요 일정 (연분홍 배경)
 */
public record CalendarDayDto(
        LocalDate date,
        int day,
        boolean inMonth,
        boolean sunday,
        boolean selected,
        String lessonTitle,
        boolean materialReady,
        boolean important
) {

    public static CalendarDayDto blank(LocalDate date) {
        return new CalendarDayDto(date, date.getDayOfMonth(), false, false, false, null, false, false);
    }

    public boolean hasLesson() {
        return lessonTitle != null;
    }
}
