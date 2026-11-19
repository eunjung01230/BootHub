package com.wanted.boothub.global.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면(헤더·사이드바)에서 쓰는 공통 모델 속성.
 * 로그인 사용자 정보는 2단계에서 여기에 추가한다.
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

    /** 사이드바 활성 메뉴 표시용 현재 경로 */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getServletPath();
    }
}
