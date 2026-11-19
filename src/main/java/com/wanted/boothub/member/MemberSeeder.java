package com.wanted.boothub.member;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 시드 계정. member 테이블이 비어 있을 때만 넣는다. (비밀번호 모두 1234)
 */
@Component
@RequiredArgsConstructor
public class MemberSeeder implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "1234";

    private final MemberService memberService;

    @Override
    public void run(String... args) {
        if (memberService.hasAnyMember()) {
            return;
        }
        memberService.create("admin", DEFAULT_PASSWORD, "박지훈", Role.ADMIN);
        memberService.create("instructor", DEFAULT_PASSWORD, "이서연", Role.INSTRUCTOR);
        memberService.create("student1", DEFAULT_PASSWORD, "김민준", Role.STUDENT);
        memberService.create("student2", DEFAULT_PASSWORD, "최지우", Role.STUDENT);
        memberService.create("student3", DEFAULT_PASSWORD, "정하윤", Role.STUDENT);
    }
}
