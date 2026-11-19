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

/**
 * 관리자가 발급한 계정. 회원가입은 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    /** BCrypt 로 인코딩된 비밀번호 */
    @Column(nullable = false)
    private String password;

    /** 화면에 표시하는 실명 */
    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private Member(String loginId, String encodedPassword, String name, Role role) {
        this.loginId = loginId;
        this.password = encodedPassword;
        this.name = name;
        this.role = role;
    }

    public static Member create(String loginId, String encodedPassword, String name, Role role) {
        return new Member(loginId, encodedPassword, name, role);
    }
}
