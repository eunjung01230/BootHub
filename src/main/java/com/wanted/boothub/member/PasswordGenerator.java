package com.wanted.boothub.member;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 계정 생성·초기화 때 쓰는 10자 임시 비밀번호 (영문 대소문자 + 숫자, 각 1자 이상).
 * 관리자가 수강생에게 불러 주거나 옮겨 적기 쉽도록 헷갈리는 글자(0 O o 1 l I)는 뺀다.
 */
@Component
public class PasswordGenerator {

    public static final int LENGTH = 10;

    private static final String LETTERS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String ALPHABET = LETTERS + DIGITS;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        while (true) {
            StringBuilder password = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                password.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            // 영문만 또는 숫자만 나온 경우는 다시 뽑는다 (치우침 없이 조건을 맞추는 가장 단순한 방법)
            if (hasAny(password, LETTERS) && hasAny(password, DIGITS)) {
                return password.toString();
            }
        }
    }

    private static boolean hasAny(CharSequence value, String chars) {
        return value.chars().anyMatch(c -> chars.indexOf(c) >= 0);
    }
}
