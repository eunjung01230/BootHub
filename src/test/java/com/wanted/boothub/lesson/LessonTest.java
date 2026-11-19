package com.wanted.boothub.lesson;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LessonTest {

    @Test
    void 자료가_하나라도_있으면_자료_준비_완료다() {
        Lesson lesson = Lesson.create(LocalDate.of(2026, 11, 2), "HTML 기초", null, null, false);
        assertThat(lesson.isMaterialReady()).isFalse();

        lesson.addResource(ResourceType.ARTIFACT, "정리 자료", "https://example.com/a");
        lesson.addResource(ResourceType.ETC, "참고", "https://example.com/b");

        assertThat(lesson.isMaterialReady()).isTrue();
        assertThat(lesson.getResources()).extracting(LessonResource::getSortOrder).containsExactly(0, 1);
        assertThat(lesson.firstResourceOf(ResourceType.ARTIFACT)).get()
                .extracting(LessonResource::getUrl).isEqualTo("https://example.com/a");
    }

    @Test
    void GitHub_주소가_비어_있으면_없는_것으로_본다() {
        assertThat(Lesson.create(LocalDate.of(2026, 11, 2), "t", null, " ", false).hasGithubUrl()).isFalse();
        assertThat(Lesson.create(LocalDate.of(2026, 11, 2), "t", null, "https://github.com/x", false).hasGithubUrl()).isTrue();
    }
}
