package com.wanted.boothub.lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 2026년 11월 시드 수업 (와이어프레임 캘린더와 동일). lesson 테이블이 비어 있을 때만 넣는다.
 * 11/2~11/19 수업에는 자료 링크를 넣어 캘린더에 초록 점이 보이게 한다.
 * 링크는 예시용 공개 문서·저장소이며, 실제 수업 자료는 관리자 화면(5단계)에서 등록한다.
 */
@Component
@RequiredArgsConstructor
public class LessonSeeder implements CommandLineRunner {

    private static final String WEB_REPO = "https://github.com/mdn/learning-area";
    private static final String REACT_REPO = "https://github.com/reactjs/react.dev";
    private static final String MDN = "https://developer.mozilla.org/ko/docs/";
    private static final String REACT_DOCS = "https://ko.react.dev/learn";

    private final LessonService lessonService;

    @Override
    public void run(String... args) {
        if (lessonService.hasAnyLesson()) {
            return;
        }
        List<Lesson> lessons = new ArrayList<>();

        lessons.add(withMaterial(2, "HTML 기초", "문서 구조와 시맨틱 태그로 첫 페이지를 만든다.", WEB_REPO, MDN + "Web/HTML"));
        lessons.add(withMaterial(3, "CSS 기초", "선택자, 박스 모델, 색과 글꼴을 다룬다.", WEB_REPO, MDN + "Web/CSS"));
        lessons.add(withMaterial(4, "Flexbox", "1차원 정렬로 내비게이션과 카드 목록을 배치한다.", WEB_REPO, MDN + "Web/CSS/CSS_flexible_box_layout"));
        lessons.add(withMaterial(5, "Grid", "2차원 격자로 페이지 레이아웃을 잡는다.", WEB_REPO, MDN + "Web/CSS/CSS_grid_layout"));
        lessons.add(withMaterial(6, "반응형 웹", "미디어 쿼리로 모바일·데스크톱 화면을 함께 지원한다.", WEB_REPO, MDN + "Web/CSS/CSS_media_queries"));

        lessons.add(withMaterial(9, "JavaScript", "변수, 조건문, 반복문, 함수의 기본 문법을 익힌다.", WEB_REPO, MDN + "Web/JavaScript"));
        lessons.add(withMaterial(10, "배열과 객체", "배열 메서드와 객체로 데이터를 다룬다.", WEB_REPO, MDN + "Web/JavaScript/Reference/Global_Objects/Array"));
        lessons.add(withMaterial(11, "DOM 조작", "요소를 찾고 만들고 바꾸는 방법을 익힌다.", WEB_REPO, MDN + "Web/API/Document_Object_Model"));
        lessons.add(withMaterial(12, "이벤트", "클릭·입력 이벤트로 화면과 상호작용한다.", WEB_REPO, MDN + "Web/API/EventTarget/addEventListener"));
        lessons.add(withMaterial(13, "비동기 처리", "Promise와 async/await, fetch로 데이터를 불러온다.", WEB_REPO, MDN + "Web/JavaScript/Reference/Global_Objects/Promise"));

        lessons.add(withMaterial(16, "React 기초", "React 프로젝트를 만들고 JSX를 익힌다.", REACT_REPO, REACT_DOCS));
        lessons.add(withMaterial(17, "컴포넌트", "화면을 재사용 가능한 컴포넌트로 나눈다.", REACT_REPO, REACT_DOCS + "/your-first-component"));
        lessons.add(withMaterial(18, "Props", "컴포넌트 사이에 데이터를 전달한다.", REACT_REPO, REACT_DOCS + "/passing-props-to-a-component"));
        lessons.add(withMaterial(19, "State와 Hooks", "useState, useEffect로 상태와 부수 효과를 다룬다.", REACT_REPO, REACT_DOCS + "/state-a-components-memory"));
        lessons.add(lesson(20, "API 연동", "외부 API 데이터를 불러와 화면에 그린다.", false));

        lessons.add(lesson(23, "프로젝트 기획", "팀을 꾸리고 주제와 화면 흐름을 정한다.", false));
        lessons.add(lesson(24, "프로젝트 실습", "기획한 기능을 나눠 구현한다.", false));
        lessons.add(lesson(25, "프로젝트 실습", "기능 구현을 이어가고 중간 점검을 한다.", false));
        lessons.add(lesson(26, "프로젝트 실습", "마무리 구현과 발표 준비를 한다.", false));
        lessons.add(lesson(27, "결과 발표", "팀별 결과물을 발표하고 피드백을 나눈다.", true));

        lessonService.registerAll(lessons);
    }

    private Lesson lesson(int day, String title, String description, boolean important) {
        return Lesson.create(LocalDate.of(2026, 11, day), title, description, null, important);
    }

    private Lesson withMaterial(int day, String title, String description, String githubUrl, String artifactUrl) {
        Lesson lesson = Lesson.create(LocalDate.of(2026, 11, day), title, description, githubUrl, false);
        lesson.addResource(ResourceType.ARTIFACT, title + " 정리 자료", artifactUrl);
        return lesson;
    }
}
