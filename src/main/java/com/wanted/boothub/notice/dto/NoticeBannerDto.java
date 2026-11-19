package com.wanted.boothub.notice.dto;

/**
 * 홈 필독 배너. 가장 최근 필독 공지 하나를 보여주고, 나머지는 개수만 알린다.
 *
 * @param otherCount 이 공지 말고 더 있는 필독 공지 수
 */
public record NoticeBannerDto(Long id, String title, int otherCount) {
}
