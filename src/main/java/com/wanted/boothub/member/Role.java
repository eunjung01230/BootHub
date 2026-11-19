package com.wanted.boothub.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 역할. Spring Security 권한은 "ROLE_" + name() 으로 만든다.
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN("관리자"),
    INSTRUCTOR("강사"),
    PROFESSOR("교수"),
    STUDENT("수강생");

    private final String label;

    public String authority() {
        return "ROLE_" + name();
    }
}
