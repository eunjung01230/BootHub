package com.wanted.boothub.notice;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 공지 카테고리. 사이드바 공지 메뉴와 /notices?category={name()} 에 대응한다.
 */
@Getter
@RequiredArgsConstructor
public enum NoticeCategory {
    ATTENDANCE("출석 관련"),
    EVALUATION("평가 관련"),
    CURRICULUM("커리큘럼"),
    CONTACT("연락처"),
    QNA("공지 Q&A");

    private final String label;

    /** 쿼리 문자열을 카테고리로. 비어 있거나 모르는 값이면 빈 값 (= 전체 공지) */
    public static Optional<NoticeCategory> fromKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(category -> category.name().equals(key)).findFirst();
    }
}
