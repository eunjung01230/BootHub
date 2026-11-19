package com.wanted.boothub.lesson.dto;

import com.wanted.boothub.lesson.Lesson;

import java.time.LocalDate;

/**
 * 관리자 수업 목록의 한 줄.
 *
 * @param resourceCount 등록된 자료 링크 수 (1개 이상이면 캘린더 초록 점)
 */
public record AdminLessonRowDto(
        Long id,
        LocalDate date,
        String dateLabel,
        String title,
        boolean important,
        boolean hasGithub,
        int resourceCount
) {

    public static AdminLessonRowDto from(Lesson lesson) {
        return new AdminLessonRowDto(
                lesson.getId(),
                lesson.getLessonDate(),
                LessonDetailDto.dateLabel(lesson.getLessonDate()),
                lesson.getTitle(),
                lesson.isImportant(),
                lesson.hasGithubUrl(),
                lesson.getResources().size()
        );
    }
}
