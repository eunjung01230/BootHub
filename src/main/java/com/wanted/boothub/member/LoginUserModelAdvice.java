package com.wanted.boothub.member;

import com.wanted.boothub.member.dto.LoginUserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 로그인 사용자를 넣는다 (헤더 실명 표시·관리자 메뉴 노출용).
 * 외부 링크·현재 경로 같은 공통 속성은 global 의 GlobalModelAdvice 가 맡는다.
 */
@ControllerAdvice
public class LoginUserModelAdvice {

    /** 비로그인(로그인 화면)이면 null */
    @ModelAttribute("loginUser")
    public LoginUserDto loginUser(@AuthenticationPrincipal LoginUser loginUser) {
        return loginUser == null ? null : LoginUserDto.from(loginUser);
    }
}
