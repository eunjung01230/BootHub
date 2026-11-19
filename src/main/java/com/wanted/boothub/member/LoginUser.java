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
    private final boolean active;
    /** true 면 PasswordChangeRequiredFilter 가 /account/password 외의 화면을 막는다 */
    private final boolean mustChangePassword;

    private LoginUser(Long memberId, String loginId, String password, String name, Role role,
                      boolean active, boolean mustChangePassword) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
    }

    public static LoginUser from(Member member) {
        return new LoginUser(member.getId(), member.getLoginId(), member.getPassword(), member.getName(),
                member.getRole(), member.isActive(), member.isMustChangePassword());
    }

    /** 비밀번호를 바꾼 뒤 세션의 로그인 정보를 갈아 끼울 때 쓴다 */
    public LoginUser withPasswordChanged() {
        return new LoginUser(memberId, loginId, null, name, role, active, false);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    /** 비활성 계정은 Spring Security 가 로그인을 거부한다 (DisabledException) */
    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
