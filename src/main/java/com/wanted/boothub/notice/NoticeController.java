package com.wanted.boothub.notice;

import com.wanted.boothub.member.LoginUser;
import com.wanted.boothub.notice.dto.NoticeForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 공지 목록·상세·작성.
 * 작성(/admin/notices/new)은 ADMIN 과 INSTRUCTOR 가 함께 쓰는 화면이라 AdminController 가 아닌 여기에 둔다.
 */
@Controller
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /** category 가 없거나 모르는 값이면 전체 공지 */
    @GetMapping("/notices")
    public String list(@RequestParam(required = false) String category, Model model) {
        NoticeCategory selected = NoticeCategory.fromKey(category).orElse(null);
        model.addAttribute("categories", NoticeCategory.values());
        model.addAttribute("selectedCategory", selected);
        model.addAttribute("notices", noticeService.getList(selected));
        return "notice/list";
    }

    @GetMapping("/notices/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeService.getDetail(id));
        return "notice/detail";
    }

    /** ?category= 로 들어오면 그 카테고리를 미리 골라 둔다 (목록의 글쓰기 버튼) */
    @GetMapping("/admin/notices/new")
    public String writeForm(@RequestParam(required = false) String category, Model model) {
        model.addAttribute("noticeForm", new NoticeForm(NoticeCategory.fromKey(category).orElse(null)));
        model.addAttribute("categories", NoticeCategory.values());
        return "notice/form";
    }

    @PostMapping("/admin/notices/new")
    public String write(@Valid @ModelAttribute("noticeForm") NoticeForm form, BindingResult bindingResult,
                        @AuthenticationPrincipal LoginUser loginUser, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", NoticeCategory.values());
            return "notice/form";
        }
        Long id = noticeService.write(loginUser.getMemberId(), form.getCategory(),
                form.getTitle(), form.getContent(), form.isPinned());
        return "redirect:/notices/" + id;
    }
}
