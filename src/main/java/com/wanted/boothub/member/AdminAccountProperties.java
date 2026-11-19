package com.wanted.boothub.member;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 첫 관리자 계정 (boothub.admin.*).
 * 환경변수 BOOTHUB_ADMIN_ID, BOOTHUB_ADMIN_PASSWORD 가 relaxed binding 으로 여기에 들어온다.
 * 코드와 커밋되는 설정 파일에는 값을 쓰지 않는다.
 */
@ConfigurationProperties(prefix = "boothub.admin")
public record AdminAccountProperties(String id, String password) {

    public boolean isConfigured() {
        return id != null && !id.isBlank() && password != null && !password.isBlank();
    }
}
