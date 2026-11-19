package com.wanted.boothub.global.web;

import java.util.Arrays;

/**
 * v2 에서 열릴 기능 목록. /coming-soon?feature={key} 의 key 와 매핑된다.
 */
public enum ComingSoonFeature {

    AI_SUMMARY("ai-summary", "AI 수업 정리", "수업 코드와 주석을 바탕으로 핵심 개념을 표와 구조 그림으로 정리해 드려요."),
    QUIZ("quiz", "복습 퀴즈", "난이도별 퀴즈로 그날 배운 내용을 점검할 수 있어요."),
    MINI_PROJECT("mini-project", "미니 프로젝트", "수업 내용을 직접 적용해 볼 수 있는 복습용 미니 프로젝트 주제를 추천해 드려요."),
    LESSON_QNA("lesson-qna", "수업 Q&A", "수업별로 질문하고 답변을 모아볼 수 있어요."),
    QNA("qna", "학습 Q&A", "수강생과 강사가 함께 묻고 답하는 학습 질문 게시판이에요."),
    CHAT("chat", "번개 학습방", "주제나 질문을 걸고 바로 모여서 실시간으로 함께 공부해요."),
    SHOWCASE("showcase", "프로젝트 전시관", "수강생들이 만든 프로젝트를 모아 구경하고 응원할 수 있어요.");

    private static final String BADGE = "v2 예정";

    private final String key;
    private final String title;
    private final String description;

    ComingSoonFeature(String key, String title, String description) {
        this.key = key;
        this.title = title;
        this.description = description;
    }

    public static ComingSoonView viewOf(String key) {
        return Arrays.stream(values())
                .filter(feature -> feature.key.equals(key))
                .findFirst()
                .map(feature -> new ComingSoonView(feature.title, feature.description, BADGE))
                .orElseGet(() -> new ComingSoonView("준비 중인 기능", "더 나은 학습 경험을 위해 준비하고 있어요.", BADGE));
    }
}
