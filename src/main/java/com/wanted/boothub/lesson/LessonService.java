package com.wanted.boothub.lesson;

import com.wanted.boothub.global.exception.NotFoundException;
import com.wanted.boothub.lesson.dto.LessonDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;

    /** 홈 상세 패널용. 수업이 없는 날이면 빈 값 */
    public Optional<LessonDetailDto> findDetail(LocalDate date) {
        return lessonRepository.findByLessonDate(date).map(LessonDetailDto::from);
    }

    /** 수업 상세 화면용. 수업이 없는 날이면 404 */
    public LessonDetailDto getDetail(LocalDate date) {
        return findDetail(date)
                .orElseThrow(() -> new NotFoundException("해당 날짜에 수업이 없습니다."));
    }

    public boolean hasAnyLesson() {
        return lessonRepository.count() > 0;
    }

    /** 시드용 일괄 저장. 자료는 Lesson 의 cascade 로 함께 저장된다. */
    @Transactional
    public void registerAll(List<Lesson> lessons) {
        lessonRepository.saveAll(lessons);
    }
}
