package com.wanted.boothub.member;

import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 세션에 저장되는 로그인 사용자. 엔티티 대신 필요한 값만 복사해 둔다.
 * 인증이 끝나면 eraseCredentials() 로 비밀번호 해시를 지워 세션에 남기지 않는다.
 */
@Getter
public class LoginUser implements UserDetails, CredentialsContainer {

    private final Long memberId;
    private final String loginId;
    private String password;
    private final String name;
    private final Role role;

    private LoginUser(Member member) {
        this.memberId = member.getId();
        this.loginId = member.getLoginId();
        this.password = member.getPassword();
        this.name = member.getName();
        this.role = member.getRole();
    }

    public static LoginUser from(Member member) {
        return new LoginUser(member);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
