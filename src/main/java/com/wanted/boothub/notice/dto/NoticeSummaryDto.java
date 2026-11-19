package com.wanted.boothub.notice.dto;

import com.wanted.boothub.notice.Notice;

import java.time.format.DateTimeFormatter;

/**
 * 공지 목록의 한 줄.
 *
 * @param createdDate 작성일 (예: "2026.11.19")
 */
public record NoticeSummaryDto(
        Long id,
        String categoryLabel,
        String title,
        String authorName,
        String createdDate,
        boolean pinned
) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static NoticeSummaryDto from(Notice notice) {
        return new NoticeSummaryDto(
                notice.getId(),
                notice.getCategory().getLabel(),
                notice.getTitle(),
                notice.getAuthor().getName(),
                notice.getCreatedAt().format(DATE),
                notice.isPinned()
        );
    }
}
