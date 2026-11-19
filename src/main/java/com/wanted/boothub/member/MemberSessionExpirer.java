package com.wanted.boothub.member;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 회원의 로그인 세션을 끊는다 (계정 비활성화·비밀번호 초기화 시).
 *
 * 로그인 세션은 SessionRegistry 에 기록된다 (SecurityConfig 의 sessionManagement).
 * 여기서 만료 표시만 해 두면, 그 세션의 다음 요청에서 ConcurrentSessionFilter 가 로그아웃시키고
 * /login?expired 로 보낸다.
 *
 * 로그인할 때마다 LoginUser 인스턴스가 새로 생기므로 principal 객체가 아니라 memberId 로 찾는다.
 * 세션 기록은 서버 메모리에 있다 (서버 1대 기준. 여러 대로 늘리면 Spring Session 등으로 옮겨야 한다).
 *
 * 지금 요청을 보낸 세션은 끊지 않는다. 관리자가 자기 비밀번호를 초기화하면 임시 비밀번호를
 * 이 세션의 화면에 한 번만 보여줘야 하므로, 여기서 끊으면 비밀번호를 모른 채 로그아웃된다.
 */
@Component
@RequiredArgsConstructor
public class MemberSessionExpirer {

    private final SessionRegistry sessionRegistry;

    /** @return 끊은 세션 수 (지금 요청의 세션은 빼고) */
    public int expireAll(Long memberId) {
        String currentSessionId = currentSessionId();
        int expired = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof LoginUser loginUser && memberId.equals(loginUser.getMemberId())) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    if (!session.getSessionId().equals(currentSessionId)) {
                        session.expireNow();
                        expired++;
                    }
                }
            }
        }
        return expired;
    }

    /** 웹 요청이 아니거나(테스트·시드) 세션이 없으면 null */
    private static String currentSessionId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpSession session = attributes.getRequest().getSession(false);
            return session == null ? null : session.getId();
        }
        return null;
    }
}
