package com.wanted.boothub.notice.dto;

import com.wanted.boothub.notice.Notice;
import com.wanted.boothub.notice.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공지 작성 폼. 화면 바인딩용이라 Setter 를 둔다 (엔티티 아님).
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticeForm {

    @NotNull(message = "카테고리를 골라 주세요.")
    private NoticeCategory category;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = Notice.TITLE_MAX, message = "제목은 {max}자까지 쓸 수 있어요.")
    private String title;

    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(max = Notice.CONTENT_MAX, message = "내용은 {max}자까지 쓸 수 있어요.")
    private String content;

    /** 필독 고정 (홈 배너·목록 맨 위) */
    private boolean pinned;

    public NoticeForm(NoticeCategory category) {
        this.category = category;
    }
}
