package com.wanted.boothub.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 수업 자료 저장소. 자료 추가는 Lesson.addResource(...) 의 cascade 로 저장되며,
 * 자료 단건 조회·삭제가 필요한 5단계 관리자 기능에서 사용한다.
 */
public interface LessonResourceRepository extends JpaRepository<LessonResource, Long> {
}
