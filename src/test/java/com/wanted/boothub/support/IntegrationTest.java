package com.wanted.boothub.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 통합 테스트 공통 설정. 모든 테스트가 같은 스프링 컨텍스트를 재사용한다.
 *  - local: 연습용 계정(instructor, student1~3) 시드
 *  - test : 메모리 DB
 *  - 관리자 계정·외부 링크는 테스트용 더미 값. @TestPropertySource 는 OS 환경변수보다 우선하므로
 *    개발 PC 에 BOOTHUB_* 환경변수가 있어도 테스트 결과가 바뀌지 않는다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@TestPropertySource(properties = {
        "boothub.admin.id=" + IntegrationTest.ADMIN_ID,
        "boothub.admin.password=" + IntegrationTest.ADMIN_PASSWORD,
        "boothub.links.slack=https://slack.example.test",
        "boothub.links.github=https://github.example.test",
        "boothub.links.notion="
})
public @interface IntegrationTest {

    String ADMIN_ID = "test-admin";
    String ADMIN_PASSWORD = "test-admin-pw";
}
