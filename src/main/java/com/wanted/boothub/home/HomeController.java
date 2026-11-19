package com.wanted.boothub.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 홈 화면. 필독 배너·캘린더·상세 패널은 3~4단계에서 채운다.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home/index";
    }
}
