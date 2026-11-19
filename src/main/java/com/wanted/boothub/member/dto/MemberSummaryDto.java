package com.wanted.boothub.member.dto;

import com.wanted.boothub.member.Member;

import java.time.format.DateTimeFormatter;

/**
 * 관리자 회원 목록의 한 줄.
 *
 * @param self 로그인한 관리자 본인 (본인은 비활성화 버튼을 숨긴다)
 */
public record MemberSummaryDto(
        Long id,
        String loginId,
        String name,
        String roleLabel,
        boolean active,
        boolean mustChangePassword,
        String createdDate,
        boolean self
) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static MemberSummaryDto of(Member member, Long currentMemberId) {
        return new MemberSummaryDto(
                member.getId(),
                member.getLoginId(),
                member.getName(),
                member.getRole().getLabel(),
                member.isActive(),
                member.isMustChangePassword(),
                member.getCreatedAt() == null ? "" : member.getCreatedAt().format(DATE),
                member.getId().equals(currentMemberId)
        );
    }
}
