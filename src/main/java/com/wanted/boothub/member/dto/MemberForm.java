package com.wanted.boothub.member.dto;

import com.wanted.boothub.member.Member;
import com.wanted.boothub.member.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 계정 생성 폼. 비밀번호는 입력받지 않고 시스템이 발급한다.
 * 화면 바인딩용이라 Setter 를 둔다 (엔티티 아님).
 */
@Getter
@Setter
@NoArgsConstructor
public class MemberForm {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Pattern(regexp = Member.LOGIN_ID_REGEX, message = "아이디는 영문 소문자, 숫자, 하이픈(-)으로 4~20자여야 해요.")
    private String loginId;

    @NotBlank(message = "실명을 입력해 주세요.")
    @Size(max = Member.NAME_MAX, message = "실명은 {max}자까지 쓸 수 있어요.")
    private String name;

    @NotNull(message = "역할을 골라 주세요.")
    private Role role;

    /** 앞뒤 공백은 실수로 보고 지운다. 대문자 등 규칙 위반은 그대로 두어 오류로 알린다. */
    public void setLoginId(String loginId) {
        this.loginId = loginId == null ? null : loginId.strip();
    }
}
