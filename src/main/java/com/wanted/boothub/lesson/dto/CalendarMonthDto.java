package com.wanted.boothub.lesson.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 월간 캘린더 화면 데이터.
 *
 * @param label        제목 (예: "2026년 11월")
 * @param month        현재 월 쿼리 값 (예: "2026-11")
 * @param prevMonth    이전 달 쿼리 값
 * @param nextMonth    다음 달 쿼리 값
 * @param selectedDate 선택된 날짜 (상세 패널 기준)
 * @param weeks        일요일부터 시작하는 주 단위 칸 목록 (주마다 7칸)
 */
public record CalendarMonthDto(
        String label,
        String month,
        String prevMonth,
        String nextMonth,
        LocalDate selectedDate,
        List<List<CalendarDayDto>> weeks
) {
}
