package com.wanted.boothub.member;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordGeneratorTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    @Test
    void 영문과_숫자를_각각_하나_이상_넣은_10자를_만든다() {
        for (int i = 0; i < 1_000; i++) {
            String password = generator.generate();
            assertThat(password)
                    .hasSize(PasswordGenerator.LENGTH)
                    .matches("[A-Za-z0-9]+")
                    .containsPattern("[A-Za-z]")
                    .containsPattern("[0-9]")
                    .doesNotContainPattern("[0Oo1lI]");
        }
    }

    @Test
    void 매번_다른_비밀번호를_만든다() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            passwords.add(generator.generate());
        }
        assertThat(passwords).hasSize(1_000);
    }

    @Test
    void 발급한_임시_비밀번호는_사용자_비밀번호_규칙도_지킨다() {
        for (int i = 0; i < 1_000; i++) {
            assertThat(PasswordPolicy.violation("s3-017", generator.generate())).isEmpty();
        }
    }
}
