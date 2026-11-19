package com.wanted.boothub.global.web;

import com.wanted.boothub.member.LoginUser;
import com.wanted.boothub.member.dto.LoginUserDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면(헤더·사이드바)에서 쓰는 공통 모델 속성.
 */
@ControllerAdvice
@RequiredArgsConstructor
@EnableConfigurationProperties(ExternalLinkProperties.class)
public class GlobalModelAdvice {

    private final ExternalLinkProperties externalLinkProperties;

    @ModelAttribute("externalLinks")
    public ExternalLinkProperties externalLinks() {
        return externalLinkProperties;
    }

    /** 헤더 실명 표시·관리자 메뉴 노출용. 비로그인(로그인 화면)이면 null */
    @ModelAttribute("loginUser")
    public LoginUserDto loginUser(@AuthenticationPrincipal LoginUser loginUser) {
        return loginUser == null ? null : LoginUserDto.from(loginUser);
    }

    /** 사이드바 활성 메뉴 표시용 현재 경로 */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getServletPath();
    }
}
