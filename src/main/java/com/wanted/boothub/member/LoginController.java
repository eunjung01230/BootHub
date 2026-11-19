package com.wanted.boothub.member;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그인 화면. 인증 처리(POST /login)와 로그아웃은 Spring Security 가 맡는다.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }
}
