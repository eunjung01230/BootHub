package com.wanted.boothub.notice;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 전체 공지 목록. 작성자 실명을 함께 보여주므로 author 를 같이 가져온다. */
    @EntityGraph(attributePaths = "author")
    List<Notice> findAllBy(Sort sort);

    @EntityGraph(attributePaths = "author")
    List<Notice> findByCategory(NoticeCategory category, Sort sort);

    @EntityGraph(attributePaths = "author")
    Optional<Notice> findWithAuthorById(Long id);

    /** 홈 필독 배너용 */
    List<Notice> findByPinnedTrue(Sort sort);
}
