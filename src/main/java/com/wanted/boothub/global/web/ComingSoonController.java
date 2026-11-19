package com.wanted.boothub.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * v2 기능 공용 "준비 중" 페이지.
 */
@Controller
public class ComingSoonController {

    @GetMapping("/coming-soon")
    public String comingSoon(@RequestParam(required = false) String feature, Model model) {
        model.addAttribute("comingSoon", ComingSoonFeature.viewOf(feature));
        return "common/coming-soon";
    }
}
