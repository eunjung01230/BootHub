package com.wanted.boothub.lesson.dto;

import com.wanted.boothub.lesson.Lesson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 관리자 수업 등록·수정 폼. 화면 바인딩용이라 Setter 를 둔다 (엔티티 아님).
 * 수정 화면에서 날짜는 바꿀 수 없다 (날짜가 수업의 식별 기준). 숨은 값으로만 넘어온다.
 */
@Getter
@Setter
@NoArgsConstructor
public class LessonForm {

    /** http(s) 주소만 받는다 (javascript: 같은 주소가 링크로 그려지지 않도록) */
    public static final String URL_REGEX = "^https?://\\S+$";
    public static final String URL_MESSAGE = "http:// 또는 https:// 로 시작하는 주소를 넣어 주세요.";

    @NotNull(message = "날짜를 골라 주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lessonDate;

    @NotBlank(message = "수업명을 입력해 주세요.")
    @Size(max = Lesson.TITLE_MAX, message = "수업명은 {max}자까지 쓸 수 있어요.")
    private String title;

    @Size(max = Lesson.DESCRIPTION_MAX, message = "설명은 {max}자까지 쓸 수 있어요.")
    private String description;

    @Size(max = Lesson.URL_MAX, message = "주소는 {max}자까지 쓸 수 있어요.")
    @Pattern(regexp = "^$|" + URL_REGEX, message = URL_MESSAGE)
    private String githubUrl;

    /** 중요 일정 (캘린더 연분홍 배경) */
    private boolean important;

    public LessonForm(LocalDate lessonDate) {
        this.lessonDate = lessonDate;
    }

    public static LessonForm from(Lesson lesson) {
        LessonForm form = new LessonForm(lesson.getLessonDate());
        form.title = lesson.getTitle();
        form.description = lesson.getDescription();
        form.githubUrl = lesson.getGithubUrl();
        form.important = lesson.isImportant();
        return form;
    }
}
