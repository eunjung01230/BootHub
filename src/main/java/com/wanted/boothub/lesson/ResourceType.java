package com.wanted.boothub.lesson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 수업 자료 종류. v2 에서 AI_SUMMARY, QUIZ, MINI_PROJECT 를 추가해 확장한다.
 */
@Getter
@RequiredArgsConstructor
public enum ResourceType {
    ARTIFACT("강사 아티팩트"),
    GITHUB("GitHub"),
    ETC("기타");

    private final String label;
}
