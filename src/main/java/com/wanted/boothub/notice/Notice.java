package com.wanted.boothub.notice;

import com.wanted.boothub.global.common.BaseTimeEntity;
import com.wanted.boothub.member.Member;
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
 * 공지글. 필독(pinned) 공지는 목록 맨 위와 홈 배너에 보인다.
 * 작성 시각은 BaseTimeEntity.createdAt 을 쓴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    public static final int TITLE_MAX = 100;
    public static final int CONTENT_MAX = 5000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeCategory category;

    @Column(nullable = false, length = TITLE_MAX)
    private String title;

    @Column(nullable = false, length = CONTENT_MAX)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    /** 필독 고정 */
    @Column(nullable = false)
    private boolean pinned;

    private Notice(NoticeCategory category, String title, String content, Member author) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public static Notice create(NoticeCategory category, String title, String content, Member author) {
        return new Notice(category, title, content, author);
    }

    public void pin() {
        this.pinned = true;
    }

    public void unpin() {
        this.pinned = false;
    }
}
