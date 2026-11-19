package com.wanted.boothub.member;

import com.wanted.boothub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 관리자가 발급한 계정. 회원가입은 없다.
 * 아이디는 생성 후 바꿀 수 없다 (바꾸는 메서드를 두지 않는다).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    /** 아이디 규칙: 영문 소문자·숫자·하이픈 4~20자. 형식(역할-기수-번호)은 관례로 안내한다. */
    public static final String LOGIN_ID_REGEX = "^[a-z0-9-]{4,20}$";
    public static final int NAME_MAX = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 20)
    private String loginId;

    /** BCrypt 로 인코딩된 비밀번호 */
    @Column(nullable = false)
    private String password;

    /** 화면에 표시하는 실명 */
    @Column(nullable = false, length = NAME_MAX)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** 비활성 계정은 로그인할 수 없다. 기존 행이 있는 DB 에 컬럼이 추가될 때를 위해 기본값을 둔다. */
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    /** true 면 로그인 직후 비밀번호 변경 화면으로 보내고, 바꾸기 전까지 다른 화면을 막는다. */
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean mustChangePassword;

    private Member(String loginId, String encodedPassword, String name, Role role, boolean mustChangePassword) {
        this.loginId = loginId;
        this.password = encodedPassword;
        this.name = name;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    /** @param mustChangePassword 시스템이 발급한 비밀번호면 true, 환경변수·연습용 시드면 false */
    public static Member create(String loginId, String encodedPassword, String name, Role role,
                                boolean mustChangePassword) {
        return new Member(loginId, encodedPassword, name, role, mustChangePassword);
    }

    /** 관리자 초기화. 새 임시 비밀번호로 바꾸고 다음 로그인 때 변경을 강제한다. */
    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
        this.mustChangePassword = true;
    }

    /** 본인 변경. 변경 강제를 푼다. */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.mustChangePassword = false;
    }

    public void deactivate() {
        this.active = false;
    }
}
