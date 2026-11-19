package com.wanted.boothub.lesson;

import com.wanted.boothub.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 하루 한 개의 수업. 날짜가 곧 식별 기준이다 (lessonDate unique).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate lessonDate;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String githubUrl;

    /** 중요 일정 (캘린더 연분홍 배경) */
    @Column(nullable = false)
    private boolean important;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<LessonResource> resources = new ArrayList<>();

    private Lesson(LocalDate lessonDate, String title, String description, String githubUrl, boolean important) {
        this.lessonDate = lessonDate;
        this.title = title;
        this.description = description;
        this.githubUrl = githubUrl;
        this.important = important;
    }

    public static Lesson create(LocalDate lessonDate, String title, String description,
                                String githubUrl, boolean important) {
        return new Lesson(lessonDate, title, description, githubUrl, important);
    }

    public void update(String title, String description, String githubUrl, boolean important) {
        this.title = title;
        this.description = description;
        this.githubUrl = githubUrl;
        this.important = important;
    }

    /** 자료 링크 추가. 정렬 순서는 추가된 순서대로 붙는다. */
    public LessonResource addResource(ResourceType type, String title, String url) {
        LessonResource resource = new LessonResource(this, type, title, url, resources.size());
        resources.add(resource);
        return resource;
    }

    /** 자료가 1개 이상이면 준비 완료 (캘린더 초록 점) */
    public boolean isMaterialReady() {
        return !resources.isEmpty();
    }

    public boolean hasGithubUrl() {
        return githubUrl != null && !githubUrl.isBlank();
    }

    public List<LessonResource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    public Optional<LessonResource> firstResourceOf(ResourceType type) {
        return resources.stream().filter(resource -> resource.getType() == type).findFirst();
    }
}
