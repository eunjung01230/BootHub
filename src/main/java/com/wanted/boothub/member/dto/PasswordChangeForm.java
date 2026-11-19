package com.wanted.boothub.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 본인 비밀번호 변경 폼. 비밀번호 규칙(PasswordPolicy)은 MemberService 가 검사한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeForm {

    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    private String newPassword;

    @NotBlank(message = "새 비밀번호를 한 번 더 입력해 주세요.")
    private String newPasswordConfirm;

    public boolean isConfirmed() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }
}
