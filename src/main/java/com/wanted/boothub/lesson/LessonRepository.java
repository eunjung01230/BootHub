package com.wanted.boothub.lesson;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** 월간 캘린더용. 초록 점 판단에 자료가 필요하므로 함께 가져온다. */
    @EntityGraph(attributePaths = "resources")
    List<Lesson> findByLessonDateBetweenOrderByLessonDateAsc(LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = "resources")
    Optional<Lesson> findByLessonDate(LocalDate lessonDate);

    /** 관리자 수업 목록용. 자료 개수를 보여주므로 함께 가져온다. */
    @EntityGraph(attributePaths = "resources")
    List<Lesson> findAllByOrderByLessonDateAsc();

    @EntityGraph(attributePaths = "resources")
    Optional<Lesson> findWithResourcesById(Long id);

    boolean existsByLessonDate(LocalDate lessonDate);
}
