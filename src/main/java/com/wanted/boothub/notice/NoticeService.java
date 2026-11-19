package com.wanted.boothub.notice;

import com.wanted.boothub.global.exception.NotFoundException;
import com.wanted.boothub.member.Member;
import com.wanted.boothub.member.MemberRepository;
import com.wanted.boothub.notice.dto.NoticeBannerDto;
import com.wanted.boothub.notice.dto.NoticeDetailDto;
import com.wanted.boothub.notice.dto.NoticeSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    /** 필독 먼저, 그다음 최신순 */
    private static final Sort LIST_ORDER = Sort.by(
            Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final Sort LATEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;

    /** @param category 없으면 전체 공지 */
    public List<NoticeSummaryDto> getList(NoticeCategory category) {
        List<Notice> notices = category == null
                ? noticeRepository.findAllBy(LIST_ORDER)
                : noticeRepository.findByCategory(category, LIST_ORDER);
        return notices.stream().map(NoticeSummaryDto::from).toList();
    }

    public NoticeDetailDto getDetail(Long id) {
        return noticeRepository.findWithAuthorById(id)
                .map(NoticeDetailDto::from)
                .orElseThrow(() -> new NotFoundException("공지를 찾을 수 없습니다."));
    }

    /** 홈 필독 배너. 필독 공지가 없으면 빈 값 */
    public Optional<NoticeBannerDto> findBanner() {
        List<Notice> pinned = noticeRepository.findByPinnedTrue(LATEST);
        if (pinned.isEmpty()) {
            return Optional.empty();
        }
        Notice latest = pinned.getFirst();
        return Optional.of(new NoticeBannerDto(latest.getId(), latest.getTitle(), pinned.size() - 1));
    }

    public boolean hasAnyNotice() {
        return noticeRepository.count() > 0;
    }

    /** 공지 작성. 권한(ADMIN, INSTRUCTOR)은 SecurityConfig 의 /admin/notices/** 규칙이 맡는다. */
    @Transactional
    public Long write(Long authorId, NoticeCategory category, String title, String content, boolean pinned) {
        Member author = memberRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("작성자를 찾을 수 없습니다."));
        Notice notice = Notice.create(category, title.strip(), content.strip(), author);
        if (pinned) {
            notice.pin();
        }
        return noticeRepository.save(notice).getId();
    }
}
