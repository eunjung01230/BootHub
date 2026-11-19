package com.wanted.boothub.admin;

import com.wanted.boothub.global.exception.BusinessException;
import com.wanted.boothub.lesson.LessonService;
import com.wanted.boothub.lesson.ResourceType;
import com.wanted.boothub.lesson.dto.LessonForm;
import com.wanted.boothub.lesson.dto.LessonResourceForm;
import com.wanted.boothub.member.LoginUser;
import com.wanted.boothub.member.MemberService;
import com.wanted.boothub.member.Role;
import com.wanted.boothub.member.dto.IssuedPasswordDto;
import com.wanted.boothub.member.dto.MemberForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * 관리자 화면 (/admin/**, ADMIN 전용 — SecurityConfig). 로직은 각 도메인 Service 가 맡고,
 * 여기서는 폼 검증 결과와 BusinessException 을 화면 오류로 옮기는 일만 한다.
 * 공지 작성(/admin/notices/new)은 강사도 쓰므로 NoticeController 에 있다.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String FLASH = "flashMessage";

    private final LessonService lessonService;
    private final MemberService memberService;

    // ---------- 수업 ----------

    @GetMapping("/lessons")
    public String lessons(Model model) {
        model.addAttribute("lessons", lessonService.getAdminList());
        return "admin/lessons";
    }

    /** ?date=2026-11-30 으로 들어오면 날짜를 미리 채운다 */
    @GetMapping("/lessons/new")
    public String newLessonForm(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                Model model) {
        model.addAttribute("lessonForm", new LessonForm(date));
        return "admin/lesson-form";
    }

    /** 등록 후 바로 자료 링크를 붙일 수 있게 수정 화면으로 보낸다 */
    @PostMapping("/lessons/new")
    public String createLesson(@Valid @ModelAttribute("lessonForm") LessonForm form, BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/lesson-form";
        }
        Long id;
        try {
            id = lessonService.register(form.getLessonDate(), form.getTitle(), form.getDescription(),
                    form.getGithubUrl(), form.isImportant());
        } catch (BusinessException e) {
            reject(bindingResult, e);
            return "admin/lesson-form";
        }
        redirectAttributes.addFlashAttribute(FLASH, "수업을 등록했어요. 이제 자료 링크를 추가할 수 있어요.");
        return "redirect:/admin/lessons/" + id + "/edit";
    }

    @GetMapping("/lessons/{id}/edit")
    public String editLessonForm(@PathVariable Long id, Model model) {
        model.addAttribute("lessonForm", lessonService.getForm(id));
        model.addAttribute("resourceForm", new LessonResourceForm());
        return lessonEditView(id, model);
    }

    @PostMapping("/lessons/{id}/edit")
    public String updateLesson(@PathVariable Long id,
                               @Valid @ModelAttribute("lessonForm") LessonForm form, BindingResult bindingResult,
                               Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resourceForm", new LessonResourceForm());
            return lessonEditView(id, model);
        }
        lessonService.update(id, form.getTitle(), form.getDescription(), form.getGithubUrl(), form.isImportant());
        redirectAttributes.addFlashAttribute(FLASH, "수업 정보를 저장했어요.");
        return "redirect:/admin/lessons/" + id + "/edit";
    }

    @PostMapping("/lessons/{id}/resources")
    public String addResource(@PathVariable Long id,
                              @Valid @ModelAttribute("resourceForm") LessonResourceForm form,
                              BindingResult bindingResult,
                              Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("lessonForm", lessonService.getForm(id));
            return lessonEditView(id, model);
        }
        lessonService.addResource(id, form.getType(), form.getTitle(), form.getUrl());
        redirectAttributes.addFlashAttribute(FLASH, "자료 링크를 추가했어요.");
        return "redirect:/admin/lessons/" + id + "/edit";
    }

    private String lessonEditView(Long id, Model model) {
        model.addAttribute("lessonId", id);
        model.addAttribute("lesson", lessonService.getDetailById(id));
        model.addAttribute("resourceTypes", ResourceType.values());
        return "admin/lesson-form";
    }

    // ---------- 회원 ----------

    @GetMapping("/members")
    public String members(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("members", memberService.getMembers(loginUser.getMemberId()));
        return "admin/members";
    }

    @GetMapping("/members/new")
    public String newMemberForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());
        model.addAttribute("roles", Role.values());
        return "admin/member-form";
    }

    /** 발급한 임시 비밀번호는 flash 로 회원 목록에 한 번만 보여준다 */
    @PostMapping("/members/new")
    public String createMember(@Valid @ModelAttribute("memberForm") MemberForm form, BindingResult bindingResult,
                               Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                IssuedPasswordDto issued = memberService.issue(form.getLoginId(), form.getName(), form.getRole());
                redirectAttributes.addFlashAttribute("issuedPassword", issued);
                return "redirect:/admin/members";
            } catch (BusinessException e) {
                reject(bindingResult, e);
            }
        }
        model.addAttribute("roles", Role.values());
        return "admin/member-form";
    }

    @PostMapping("/members/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("issuedPassword", memberService.resetPassword(id));
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/deactivate")
    public String deactivate(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser,
                             RedirectAttributes redirectAttributes) {
        try {
            memberService.deactivate(id, loginUser.getMemberId());
            redirectAttributes.addFlashAttribute(FLASH, "계정을 비활성화했어요. 이제 이 계정으로 로그인할 수 없어요.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/admin/members";
    }

    private void reject(BindingResult bindingResult, BusinessException e) {
        if (e.getField() != null) {
            bindingResult.rejectValue(e.getField(), "invalid", e.getMessage());
        } else {
            bindingResult.reject("invalid", e.getMessage());
        }
    }
}
