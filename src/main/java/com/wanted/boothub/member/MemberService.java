package com.wanted.boothub.member;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean hasAnyMember() {
        return memberRepository.count() > 0;
    }

    /** 계정 발급. 비밀번호는 BCrypt 로 인코딩해 저장한다. */
    @Transactional
    public Long create(String loginId, String rawPassword, String name, Role role) {
        Member member = Member.create(loginId, passwordEncoder.encode(rawPassword), name, role);
        return memberRepository.save(member).getId();
    }
}
