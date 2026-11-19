package com.wanted.boothub.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 시드 계정. member 테이블이 비어 있을 때만 넣는다.
 *  - 첫 ADMIN: 환경변수 BOOTHUB_ADMIN_ID / BOOTHUB_ADMIN_PASSWORD (모든 프로필). 없으면 앱 시작 실패
 *  - 연습용 instructor, student1~3: local 프로필에서만 (비밀번호 1234)
 * 다른 시드(공지 작성자 등)가 회원을 쓰므로 가장 먼저 실행한다.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminAccountProperties.class)
public class MemberSeeder implements CommandLineRunner {

    private static final String ADMIN_NAME = "관리자";
    private static final String PRACTICE_PASSWORD = "1234";

    private final MemberService memberService;
    private final AdminAccountProperties adminAccount;
    private final Environment environment;

    @Override
    public void run(String... args) {
        if (memberService.hasAnyMember()) {
            return;
        }
        createAdmin();
        if (environment.acceptsProfiles(Profiles.of("local"))) {
            createPracticeAccounts();
        }
    }

    private void createAdmin() {
        if (!adminAccount.isConfigured()) {
            String reason = "회원 테이블이 비어 있어 첫 관리자 계정을 만들어야 하는데, "
                    + "환경변수 BOOTHUB_ADMIN_ID, BOOTHUB_ADMIN_PASSWORD 가 설정되지 않았습니다. "
                    + "두 환경변수를 설정한 뒤 다시 실행하세요.";
            log.error(reason);
            throw new IllegalStateException(reason);
        }
        memberService.create(adminAccount.id(), adminAccount.password(), ADMIN_NAME, Role.ADMIN);
        log.info("첫 관리자 계정을 만들었습니다. (아이디: {})", adminAccount.id());
    }

    private void createPracticeAccounts() {
        memberService.create("instructor", PRACTICE_PASSWORD, "이서연", Role.INSTRUCTOR);
        memberService.create("student1", PRACTICE_PASSWORD, "김민준", Role.STUDENT);
        memberService.create("student2", PRACTICE_PASSWORD, "최지우", Role.STUDENT);
        memberService.create("student3", PRACTICE_PASSWORD, "정하윤", Role.STUDENT);
        log.info("[local] 연습용 계정 instructor, student1~3 을 만들었습니다.");
    }
}
