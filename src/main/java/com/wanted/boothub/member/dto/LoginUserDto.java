package com.wanted.boothub.member.dto;

import com.wanted.boothub.member.LoginUser;
import com.wanted.boothub.member.Role;

/**
 * 헤더·사이드바에서 쓰는 로그인 사용자 정보.
 *
 * @param noticeWriter 공지 글쓰기 버튼 노출 (ADMIN, INSTRUCTOR)
 */
public record LoginUserDto(String name, String roleLabel, boolean admin, boolean noticeWriter) {

    public static LoginUserDto from(LoginUser loginUser) {
        Role role = loginUser.getRole();
        return new LoginUserDto(loginUser.getName(), role.getLabel(), role == Role.ADMIN, role.canWriteNotice());
    }
}
