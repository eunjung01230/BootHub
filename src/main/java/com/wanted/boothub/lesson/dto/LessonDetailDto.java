package com.wanted.boothub.lesson.dto;

import com.wanted.boothub.lesson.Lesson;
import com.wanted.boothub.lesson.LessonResource;
import com.wanted.boothub.lesson.ResourceType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 홈 상세 패널과 수업 상세 화면 데이터.
 *
 * @param githubUrl   GitHub 코드 버튼 링크 (없으면 null → 버튼 비활성)
 * @param artifactUrl 강사 아티팩트 버튼 링크 = 첫 번째 아티팩트 (없으면 null → 버튼 비활성)
 */
public record LessonDetailDto(
        LocalDate date,
        String dateLabel,
        String title,
        String description,
        boolean important,
        boolean materialReady,
        String githubUrl,
        String artifactUrl,
        List<ResourceDto> resources
) {

    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);

    public static LessonDetailDto from(Lesson lesson) {
        return new LessonDetailDto(
                lesson.getLessonDate(),
                dateLabel(lesson.getLessonDate()),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.isImportant(),
                lesson.isMaterialReady(),
                lesson.hasGithubUrl() ? lesson.getGithubUrl() : null,
                lesson.firstResourceOf(ResourceType.ARTIFACT).map(LessonResource::getUrl).orElse(null),
                lesson.getResources().stream().map(ResourceDto::from).toList()
        );
    }

    public static String dateLabel(LocalDate date) {
        return date.format(DATE_LABEL);
    }

    public record ResourceDto(String typeLabel, String title, String url) {

        static ResourceDto from(LessonResource resource) {
            return new ResourceDto(resource.getType().getLabel(), resource.getTitle(), resource.getUrl());
        }
    }
}
