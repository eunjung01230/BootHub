package com.wanted.boothub.member;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * 사용자가 정하는 비밀번호 규칙 (/account/password).
 *  - 8자 이상, 영문과 숫자를 모두 포함
 *  - 아이디와 같거나 아이디를 포함하면 거부 (대소문자 무시)
 *  - BCrypt 가 72바이트까지만 처리하므로 그보다 길면 거부
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    private static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /** 규칙을 어기면 사용자에게 보여줄 이유, 지키면 빈 값 */
    public static Optional<String> violation(String loginId, String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return Optional.of("비밀번호는 " + MIN_LENGTH + "자 이상이어야 해요.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return Optional.of("비밀번호가 너무 길어요.");
        }
        if (!containsLetter(password) || !containsDigit(password)) {
            return Optional.of("영문과 숫자를 모두 넣어 주세요.");
        }
        if (loginId != null && password.toLowerCase(Locale.ROOT).contains(loginId.toLowerCase(Locale.ROOT))) {
            return Optional.of("아이디가 들어간 비밀번호는 쓸 수 없어요.");
        }
        return Optional.empty();
    }

    private static boolean containsLetter(String value) {
        return value.chars().anyMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    }

    private static boolean containsDigit(String value) {
        return value.chars().anyMatch(c -> c >= '0' && c <= '9');
    }
}
