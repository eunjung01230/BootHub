package com.wanted.boothub.member;

import com.wanted.boothub.global.exception.BusinessException;
import com.wanted.boothub.global.exception.NotFoundException;
import com.wanted.boothub.member.dto.IssuedPasswordDto;
import com.wanted.boothub.member.dto.MemberSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    /** 역할(ADMIN → INSTRUCTOR → PROFESSOR → STUDENT) 다음 아이디 순 */
    private static final Sort LIST_ORDER = Sort.by("role", "loginId");

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final MemberSessionExpirer memberSessionExpirer;

    public boolean hasAnyMember() {
        return memberRepository.count() > 0;
    }

    /** 시드 데이터 작성자용. 관리자는 MemberSeeder 가 항상 만들어 둔다. */
    public Long findFirstAdminId() {
        return memberRepository.findFirstByRoleOrderByIdAsc(Role.ADMIN)
                .map(Member::getId)
                .orElseThrow(() -> new IllegalStateException("관리자 계정이 없습니다."));
    }

    public static boolean isValidLoginId(String loginId) {
        return loginId != null && loginId.matches(Member.LOGIN_ID_REGEX);
    }

    /**
     * 시드 계정 생성 (환경변수 관리자, local 연습용). 비밀번호를 정해서 넣으므로 변경을 강제하지 않는다.
     * 관리자 화면의 계정 발급은 issue(...) 를 쓴다.
     */
    @Transactional
    public Long create(String loginId, String rawPassword, String name, Role role) {
        Member member = Member.create(loginId, passwordEncoder.encode(rawPassword), name, role, false);
        return memberRepository.save(member).getId();
    }

    /** 관리자 회원 목록 */
    public List<MemberSummaryDto> getMembers(Long currentMemberId) {
        return memberRepository.findAll(LIST_ORDER).stream()
                .map(member -> MemberSummaryDto.of(member, currentMemberId))
                .toList();
    }

    /**
     * 관리자 계정 발급. 임시 비밀번호를 만들어 해시만 저장하고, 평문은 돌려줘서 화면에 한 번만 보여준다.
     * 발급된 계정은 첫 로그인 때 비밀번호 변경을 강제한다.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public IssuedPasswordDto issue(String loginId, String name, Role role) {
        if (!isValidLoginId(loginId)) {
            throw new BusinessException("loginId", "아이디는 영문 소문자, 숫자, 하이픈(-)으로 4~20자여야 해요.");
        }
        if (memberRepository.existsByLoginId(loginId)) {
            throw new BusinessException("loginId", "이미 쓰고 있는 아이디예요.");
        }
        String rawPassword = passwordGenerator.generate();
        memberRepository.save(Member.create(loginId, passwordEncoder.encode(rawPassword), name.strip(), role, true));
        return new IssuedPasswordDto(loginId, name.strip(), rawPassword, "발급");
    }

    /**
     * 관리자 비밀번호 초기화. 새 임시 비밀번호를 발급하고 다음 로그인 때 변경을 강제한다.
     * 이미 로그인해 있던 세션도 끊는다 (관리자가 자기 비밀번호를 초기화한 경우 지금 세션은 남긴다).
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public IssuedPasswordDto resetPassword(Long memberId) {
        Member member = getMember(memberId);
        String rawPassword = passwordGenerator.generate();
        member.resetPassword(passwordEncoder.encode(rawPassword));
        memberSessionExpirer.expireAll(memberId);
        return new IssuedPasswordDto(member.getLoginId(), member.getName(), rawPassword, "초기화");
    }

    /**
     * 계정 비활성화. 관리자가 자기 계정을 잠가 버리는 일을 막는다.
     * 이미 로그인해 있던 세션도 끊어서, 다음 요청부터 바로 로그인 화면으로 보낸다.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(Long memberId, Long currentMemberId) {
        if (memberId.equals(currentMemberId)) {
            throw new BusinessException("본인 계정은 비활성화할 수 없어요.");
        }
        getMember(memberId).deactivate();
        memberSessionExpirer.expireAll(memberId);
    }

    /**
     * 본인 비밀번호 변경. 현재 비밀번호 확인 → 규칙 검사 → 지금 비밀번호와 다른지 확인.
     * (마지막 확인이 없으면 임시 비밀번호를 그대로 다시 넣어 변경 강제를 우회할 수 있다)
     * 로그인한 본인 계정만 바꿀 수 있다 (Controller 도 세션의 memberId 만 넘긴다).
     */
    @Transactional
    @PreAuthorize("isAuthenticated() and #memberId == principal.memberId")
    public void changePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = getMember(memberId);
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new BusinessException("currentPassword", "현재 비밀번호가 맞지 않아요.");
        }
        PasswordPolicy.violation(member.getLoginId(), newPassword).ifPresent(reason -> {
            throw new BusinessException("newPassword", reason);
        });
        if (passwordEncoder.matches(newPassword, member.getPassword())) {
            throw new BusinessException("newPassword", "지금 쓰는 비밀번호와 다른 비밀번호를 정해 주세요.");
        }
        member.changePassword(passwordEncoder.encode(newPassword));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
    }
}
