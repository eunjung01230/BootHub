package com.wanted.boothub.home;

import com.wanted.boothub.lesson.CalendarService;
import com.wanted.boothub.lesson.LessonService;
import com.wanted.boothub.lesson.dto.CalendarMonthDto;
import com.wanted.boothub.lesson.dto.LessonDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 홈: 월간 수업 캘린더 + 선택 날짜 상세 패널. (필독 배너는 4단계)
 * month·date 는 문자열로 받아 CalendarService 가 해석한다. 형식이 틀리면 이번 달로 보여준다.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CalendarService calendarService;
    private final LessonService lessonService;

    @GetMapping("/")
    public String home(@RequestParam(required = false) String month,
                       @RequestParam(required = false) String date,
                       Model model) {
        CalendarMonthDto calendar = calendarService.getMonthFromQuery(month, date);
        model.addAttribute("calendar", calendar);
        model.addAttribute("selectedDateLabel", LessonDetailDto.dateLabel(calendar.selectedDate()));
        model.addAttribute("lesson", lessonService.findDetail(calendar.selectedDate()).orElse(null));
        return "home/index";
    }
}
