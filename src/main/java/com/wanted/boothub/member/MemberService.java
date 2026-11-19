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

    /** 시드 데이터 작성자용. 관리자는 MemberSeeder 가 항상 만들어 둔다. */
    public Long findFirstAdminId() {
        return memberRepository.findFirstByRoleOrderByIdAsc(Role.ADMIN)
                .map(Member::getId)
                .orElseThrow(() -> new IllegalStateException("관리자 계정이 없습니다."));
    }

    /** 계정 발급. 비밀번호는 BCrypt 로 인코딩해 저장한다. */
    @Transactional
    public Long create(String loginId, String rawPassword, String name, Role role) {
        Member member = Member.create(loginId, passwordEncoder.encode(rawPassword), name, role);
        return memberRepository.save(member).getId();
    }
}
