package com.wanted.boothub.global.web;

/**
 * "준비 중" 페이지에 보여줄 화면용 데이터.
 *
 * @param title       기능 이름
 * @param description 기능 설명
 * @param badge       상단 배지 문구 (예: "v2 예정", "4단계 예정")
 */
public record ComingSoonView(String title, String description, String badge) {
}
