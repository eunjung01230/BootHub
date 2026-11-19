package com.wanted.boothub.member.dto;

/**
 * 시스템이 발급한 임시 비밀번호. 관리자 화면에 한 번만 보여주고 저장하지 않는다.
 * (flash attribute 로 다음 화면에 한 번 전달된 뒤 사라진다)
 *
 * @param action 화면 문구용 ("발급" / "초기화")
 */
public record IssuedPasswordDto(String loginId, String name, String password, String action) {

    /** 실수로 로그에 찍혀도 평문이 남지 않게 한다 */
    @Override
    public String toString() {
        return "IssuedPasswordDto[loginId=" + loginId + ", action=" + action + "]";
    }
}
