package com.wanted.boothub.member;

import com.wanted.boothub.global.exception.BusinessException;
import com.wanted.boothub.member.dto.PasswordChangeForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 본인 비밀번호 변경. mustChangePassword 계정은 바꾸기 전까지 이 화면으로만 온다.
 */
@Controller
@RequiredArgsConstructor
public class AccountController {

    private static final String VIEW = "account/password";

    private final MemberService memberService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping(PasswordChangeRequiredFilter.CHANGE_URL)
    public String passwordForm(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("passwordForm", new PasswordChangeForm());
        model.addAttribute("forced", loginUser.isMustChangePassword());
        return VIEW;
    }

    @PostMapping(PasswordChangeRequiredFilter.CHANGE_URL)
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal LoginUser loginUser,
                                 HttpServletRequest request, HttpServletResponse response,
                                 Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("forced", loginUser.isMustChangePassword());
        if (!bindingResult.hasFieldErrors("newPasswordConfirm") && !form.isConfirmed()) {
            bindingResult.rejectValue("newPasswordConfirm", "mismatch", "새 비밀번호가 서로 달라요.");
        }
        if (bindingResult.hasErrors()) {
            return VIEW;
        }
        try {
            memberService.changePassword(loginUser.getMemberId(), form.getCurrentPassword(), form.getNewPassword());
        } catch (BusinessException e) {
            bindingResult.rejectValue(e.getField(), "invalid", e.getMessage());
            return VIEW;
        }
        refreshLogin(loginUser.withPasswordChanged(), request, response);
        redirectAttributes.addFlashAttribute("flashMessage", "비밀번호를 변경했어요.");
        return "redirect:/";
    }

    /**
     * 세션의 로그인 정보를 변경 강제가 풀린 값으로 바꾼다. 비밀번호가 바뀌었으니 세션 ID 도 새로 받는다.
     * (Spring Security 6+ 는 SecurityContext 를 직접 저장해야 다음 요청에 반영된다)
     */
    private void refreshLogin(LoginUser updated, HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = strategy.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(updated, null, updated.getAuthorities()));
        strategy.setContext(context);
        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);
    }
}
