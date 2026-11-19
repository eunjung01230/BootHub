package com.wanted.boothub.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [임시] 사이드바에 있지만 아직 구현 전인 MVP 메뉴의 자리 표시 페이지.
 * 사이드바 링크는 최종 URL 을 그대로 쓰고, 해당 단계에서 실제 컨트롤러를 만들면
 * 여기의 매핑을 지운다. (지우지 않으면 매핑 중복으로 앱이 뜨지 않으므로 누락될 일이 없다)
 *  - /notices       → 4단계 NoticeController
 *  - /admin/lessons → 5단계 AdminController
 */
@Controller
public class StagePlaceholderController {

    private static final String VIEW = "common/coming-soon";

    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("comingSoon",
                new ComingSoonView("공지 센터", "카테고리별 공지 게시판이 곧 열려요.", "4단계 예정"));
        return VIEW;
    }

    @GetMapping("/admin/lessons")
    public String adminLessons(Model model) {
        model.addAttribute("comingSoon",
                new ComingSoonView("관리자", "수업 등록·수정과 아티팩트 링크 추가 화면이 곧 열려요.", "5단계 예정"));
        return VIEW;
    }
}
