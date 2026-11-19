package com.wanted.boothub.lesson;

import com.wanted.boothub.global.exception.BusinessException;
import com.wanted.boothub.global.exception.NotFoundException;
import com.wanted.boothub.lesson.dto.AdminLessonRowDto;
import com.wanted.boothub.lesson.dto.LessonDetailDto;
import com.wanted.boothub.lesson.dto.LessonForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ---------- 관리자 ----------

    /** 관리자 수업 목록 (날짜순) */
    public List<AdminLessonRowDto> getAdminList() {
        return lessonRepository.findAllByOrderByLessonDateAsc().stream()
                .map(AdminLessonRowDto::from)
                .toList();
    }

    /** 수정 화면의 자료 목록·날짜 표시용 */
    public LessonDetailDto getDetailById(Long id) {
        return LessonDetailDto.from(getLesson(id));
    }

    /** 수정 화면 폼 초기값 */
    public LessonForm getForm(Long id) {
        return LessonForm.from(getLesson(id));
    }

    /** 수업 등록. 하루에 수업은 하나뿐이라 같은 날짜가 있으면 거부한다. */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Long register(LocalDate lessonDate, String title, String description, String githubUrl, boolean important) {
        if (lessonRepository.existsByLessonDate(lessonDate)) {
            throw new BusinessException("lessonDate", "이미 수업이 있는 날짜예요.");
        }
        Lesson lesson = Lesson.create(lessonDate, title.strip(), blankToNull(description), blankToNull(githubUrl),
                important);
        return lessonRepository.save(lesson).getId();
    }

    /** 수업 수정. 날짜는 바꾸지 않는다. */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void update(Long id, String title, String description, String githubUrl, boolean important) {
        getLesson(id).update(title.strip(), blankToNull(description), blankToNull(githubUrl), important);
    }

    /** 자료 링크 추가 (파일 업로드 없음). 링크가 하나라도 생기면 캘린더에 초록 점이 보인다. */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void addResource(Long id, ResourceType type, String title, String url) {
        getLesson(id).addResource(type, title.strip(), url.strip());
    }

    private Lesson getLesson(Long id) {
        return lessonRepository.findWithResourcesById(id)
                .orElseThrow(() -> new NotFoundException("수업을 찾을 수 없습니다."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
