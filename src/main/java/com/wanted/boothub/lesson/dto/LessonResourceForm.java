package com.wanted.boothub.lesson.dto;

import com.wanted.boothub.lesson.Lesson;
import com.wanted.boothub.lesson.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수업 자료 링크 추가 폼. 파일 업로드는 없고 URL 만 받는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class LessonResourceForm {

    @NotNull(message = "종류를 골라 주세요.")
    private ResourceType type = ResourceType.ARTIFACT;

    @NotBlank(message = "자료 이름을 입력해 주세요.")
    @Size(max = Lesson.TITLE_MAX, message = "자료 이름은 {max}자까지 쓸 수 있어요.")
    private String title;

    @NotBlank(message = "주소를 입력해 주세요.")
    @Size(max = Lesson.URL_MAX, message = "주소는 {max}자까지 쓸 수 있어요.")
    @Pattern(regexp = LessonForm.URL_REGEX, message = LessonForm.URL_MESSAGE)
    private String url;
}
