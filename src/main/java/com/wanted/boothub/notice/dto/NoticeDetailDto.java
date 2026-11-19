package com.wanted.boothub.notice.dto;

import com.wanted.boothub.notice.Notice;

import java.time.format.DateTimeFormatter;

/**
 * 공지 상세 화면 데이터.
 *
 * @param categoryKey   목록으로 돌아갈 때 쓰는 쿼리 값 (예: "EVALUATION")
 * @param createdAtLabel 작성 시각 (예: "2026.11.19 10:30")
 */
public record NoticeDetailDto(
        Long id,
        String categoryKey,
        String categoryLabel,
        String title,
        String content,
        String authorName,
        String createdAtLabel,
        boolean pinned
) {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static NoticeDetailDto from(Notice notice) {
        return new NoticeDetailDto(
                notice.getId(),
                notice.getCategory().name(),
                notice.getCategory().getLabel(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAuthor().getName(),
                notice.getCreatedAt().format(DATE_TIME),
                notice.isPinned()
        );
    }
}
