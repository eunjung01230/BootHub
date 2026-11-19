package com.wanted.boothub.member;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private static final String LOGIN_ID = "s3-017";

    @Test
    void 영문과_숫자가_있는_8자_이상_비밀번호는_통과한다() {
        assertThat(PasswordPolicy.violation(LOGIN_ID, "spring2026")).isEmpty();
        assertThat(PasswordPolicy.violation(LOGIN_ID, "Ab3!한글도ok")).isEmpty();
    }

    @Test
    void 여덟_자보다_짧으면_거부한다() {
        assertThat(PasswordPolicy.violation(LOGIN_ID, "abc1234")).hasValue("비밀번호는 8자 이상이어야 해요.");
        assertThat(PasswordPolicy.violation(LOGIN_ID, null)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "12345678", "!!!!@@@@"})
    void 영문과_숫자를_모두_넣지_않으면_거부한다(String password) {
        assertThat(PasswordPolicy.violation(LOGIN_ID, password)).hasValue("영문과 숫자를 모두 넣어 주세요.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"s3-017ab", "my-S3-017-pw1", "S3-017S3-017"})
    void 아이디와_같거나_아이디를_포함하면_대소문자_상관없이_거부한다(String password) {
        assertThat(PasswordPolicy.violation(LOGIN_ID, password)).hasValue("아이디가 들어간 비밀번호는 쓸 수 없어요.");
    }

    @Test
    void BCrypt_한계인_72바이트를_넘으면_거부한다() {
        assertThat(PasswordPolicy.violation(LOGIN_ID, "a1".repeat(36))).isEmpty();
        assertThat(PasswordPolicy.violation(LOGIN_ID, "a1".repeat(36) + "b")).hasValue("비밀번호가 너무 길어요.");
    }
}
