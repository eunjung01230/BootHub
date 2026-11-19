package com.wanted.boothub.notice;

import com.wanted.boothub.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 시드 공지. notice 테이블이 비어 있을 때만 넣는다. 작성자는 첫 관리자.
 * 작성자가 필요하므로 MemberSeeder(@Order(0)) 다음에 실행한다.
 * 필독 공지는 "프로젝트 평가 및 제출 안내" 하나이고, 나머지는 카테고리 메뉴 확인용이다.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class NoticeSeeder implements CommandLineRunner {

    private final NoticeService noticeService;
    private final MemberService memberService;

    @Override
    public void run(String... args) {
        if (noticeService.hasAnyNotice()) {
            return;
        }
        Long adminId = memberService.findFirstAdminId();

        noticeService.create(adminId, NoticeCategory.ATTENDANCE, "출결 인정 기준 안내", """
                수업은 매일 09:00에 시작합니다.
                - 09:10 이후 입실은 지각, 수업 시간의 절반 이상 불참 시 결석으로 처리합니다.
                - 병원 진료 등 부득이한 사유는 증빙 자료를 운영진에게 제출하면 출석으로 인정합니다.
                """, false);
        noticeService.create(adminId, NoticeCategory.CURRICULUM, "11월 커리큘럼 안내", """
                11월은 웹 기초부터 React 프로젝트까지 진행합니다.
                - 1주차: HTML, CSS, Flexbox, Grid, 반응형 웹
                - 2주차: JavaScript 기초, DOM, 이벤트, 비동기 처리
                - 3주차: React 기초, 컴포넌트, Props, State와 Hooks, API 연동
                - 4주차: 팀 프로젝트 기획·실습·결과 발표
                날짜별 수업은 홈 캘린더에서 확인할 수 있어요.
                """, false);
        noticeService.create(adminId, NoticeCategory.CONTACT, "운영진 연락 방법", """
                수업·출결·행정 문의는 Slack 운영 채널로 남겨 주세요.
                평일 업무 시간 안에 순서대로 답변드립니다.
                """, false);
        noticeService.create(adminId, NoticeCategory.QNA, "공지 관련 자주 묻는 질문", """
                Q. 공지는 누가 올리나요?
                A. 관리자와 강사가 올립니다.

                Q. 필독 공지는 어디서 보나요?
                A. 홈 화면 맨 위 배너와 공지 목록 맨 위에 고정됩니다.
                """, false);
        noticeService.create(adminId, NoticeCategory.EVALUATION, "프로젝트 평가 및 제출 안내", """
                11월 27일(금) 결과 발표로 팀 프로젝트를 평가합니다.
                - 제출물: GitHub 저장소 링크, 발표 자료, 시연 영상(선택)
                - 제출 기한: 11월 26일(목) 23:59까지 팀장이 제출
                - 평가 항목: 기능 완성도, 코드 품질, 협업 과정, 발표
                발표 순서는 당일 아침에 공지합니다.
                """, true);
    }
}
