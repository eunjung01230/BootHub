package com.wanted.boothub.member;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MemberSeederTest {

    private final MemberService memberService = mock(MemberService.class);

    private MemberSeeder seeder(AdminAccountProperties admin, String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return new MemberSeeder(memberService, admin, environment);
    }

    @Test
    void 회원이_없고_환경변수가_없으면_앱_시작을_실패시킨다() {
        given(memberService.hasAnyMember()).willReturn(false);

        assertThatThrownBy(() -> seeder(new AdminAccountProperties(null, " ")).run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTHUB_ADMIN_ID")
                .hasMessageContaining("BOOTHUB_ADMIN_PASSWORD");
        verify(memberService, never()).create(anyString(), anyString(), anyString(), any());
    }

    @Test
    void 관리자_아이디가_아이디_규칙에_맞지_않으면_앱_시작을_실패시킨다() {
        given(memberService.hasAnyMember()).willReturn(false);

        assertThatThrownBy(() -> seeder(new AdminAccountProperties("Admin!", "secret")).run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTHUB_ADMIN_ID");
        verify(memberService, never()).create(anyString(), anyString(), anyString(), any());
    }

    @Test
    void 회원이_이미_있으면_환경변수가_없어도_아무것도_하지_않는다() {
        given(memberService.hasAnyMember()).willReturn(true);

        seeder(new AdminAccountProperties(null, null)).run();

        verify(memberService, never()).create(anyString(), anyString(), anyString(), any());
    }

    @Test
    void local_프로필이_아니면_관리자만_만든다() {
        given(memberService.hasAnyMember()).willReturn(false);

        seeder(new AdminAccountProperties("boss", "secret"), "prod").run();

        verify(memberService).create("boss", "secret", "관리자", Role.ADMIN);
        verify(memberService, times(1)).create(anyString(), anyString(), anyString(), any());
    }

    @Test
    void local_프로필이면_연습용_계정도_만든다() {
        given(memberService.hasAnyMember()).willReturn(false);

        seeder(new AdminAccountProperties("boss", "secret"), "local").run();

        verify(memberService).create("boss", "secret", "관리자", Role.ADMIN);
        verify(memberService).create(eq("s3-001"), eq("1234"), anyString(), eq(Role.STUDENT));
        verify(memberService, times(5)).create(anyString(), anyString(), anyString(), any());
    }
}
