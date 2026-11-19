package com.wanted.boothub.lesson;

import com.wanted.boothub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수업 자료 링크 (아티팩트, GitHub 등). 파일 업로드는 없고 URL 만 저장한다.
 * 생성은 Lesson.addResource(...) 로만 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonResource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType type;

    @Column(nullable = false, length = Lesson.TITLE_MAX)
    private String title;

    @Column(nullable = false, length = Lesson.URL_MAX)
    private String url;

    @Column(nullable = false)
    private int sortOrder;

    LessonResource(Lesson lesson, ResourceType type, String title, String url, int sortOrder) {
        this.lesson = lesson;
        this.type = type;
        this.title = title;
        this.url = url;
        this.sortOrder = sortOrder;
    }
}
