# BootHub — 프로젝트 지침

부트캠프 수강생·강사·관리자를 위한 학습 허브. **1일 MVP를 먼저 완성하고, 이후 기능을 같은 구조 위에 계속 쌓는다.**
AI 코딩 도구는 작업 전에 이 파일을 반드시 읽고, 여기 적힌 범위와 규칙을 지킨다.

---

## 1. 기술 스택

- Java 17, Spring Boot 3
- Spring Web, Thymeleaf, Spring Data JPA, Validation
- Spring Security (폼 로그인, BCrypt) — 2단계부터 적용
- H2 (file 모드: `jdbc:h2:file:./data/boothub`)
- Lombok
- 프론트: Thymeleaf + 순수 CSS (CSS 변수). 프레임워크·외부 JS 라이브러리 사용 금지

---

## 2. 범위

### MVP (지금 만든다)

| 기능          | 내용                                                                  |
|---------------|-----------------------------------------------------------------------|
| 공통 레이아웃 | 헤더(로고, Slack/GitHub/Notion 고정 링크), 좌측 사이드바, 디자인 토큰 |
| 로그인        | 회원가입 없음. 관리자가 발급한 계정만 로그인. 실명 표시               |
| 홈            | 필독 공지 배너, 월간 수업 캘린더, 선택 날짜 상세 패널                 |
| 수업          | 날짜별 수업, GitHub 링크, 강사 아티팩트 링크                          |
| 공지          | 카테고리별 게시판 (출석/평가/커리큘럼/연락처/공지 Q&A), 필독 고정     |
| 관리자        | 수업 등록·수정, 아티팩트 링크 추가, 공지 작성                         |

### v2 (지금은 만들지 않는다 — 자리만 비워둔다)

- AI 수업 정리 (코드·주석 기반 표/구조 그림)
- 난이도별 복습 퀴즈
- 복습용 미니 프로젝트 주제
- 프로젝트 전시관
- 번개 학습방 (실시간 채팅, WebSocket)
- 학습 Q&A / 수업 Q&A
- 수업 종료 시 GitHub 자동 동기화
- 세부 열람 권한 (현재는 로그인 = 열람 허가)

v2 항목은 사이드바 메뉴와 버튼만 존재하고, 누르면 "준비 중" 페이지를 보여준다.

---

## 3. 패키지 구조 (도메인별)

```
com.boothub
├── global
│   ├── config          # SecurityConfig, WebConfig
│   ├── common          # BaseTimeEntity (createdAt, updatedAt)
│   ├── exception       # 공통 예외, GlobalExceptionHandler
│   └── web             # 공통 모델 속성 (외부 링크, 로그인 사용자) ControllerAdvice
├── member
│   ├── Member, Role
│   ├── MemberRepository
│   ├── MemberService
│   └── LoginUserDetailsService
├── lesson
│   ├── Lesson, LessonResource, ResourceType
│   ├── LessonRepository, LessonResourceRepository
│   ├── LessonService
│   ├── CalendarService      # 월간 캘린더 데이터 생성
│   ├── dto                  # CalendarDayDto, LessonDetailDto
│   └── LessonController
├── notice
│   ├── Notice, NoticeCategory
│   ├── NoticeRepository
│   ├── NoticeService
│   ├── dto
│   └── NoticeController
├── admin
│   └── AdminController      # 관리자 화면 전용, 로직은 각 도메인 Service 호출
└── home
    └── HomeController
```

v2에서 추가될 패키지: `material`(AI 정리·퀴즈), `showcase`, `chat`, `qna`, `sync`. **새 기능은 새 패키지로 추가하고, 기존 도메인 코드는 최소한만 건드린다.**

### templates 구조

```
templates/
├── layout/
│   ├── base.html            # 전체 뼈대
│   ├── header.html          # fragment
│   └── sidebar.html         # fragment
├── home/index.html
├── lesson/detail.html
├── notice/list.html, detail.html, form.html
├── admin/lessons.html, lesson-form.html
├── auth/login.html
└── common/coming-soon.html  # v2 기능 공용 "준비 중"
static/css/
├── tokens.css               # 디자인 토큰만
└── app.css                  # 컴포넌트 스타일
```

---

## 4. 도메인 모델

```
Member
  id, loginId(unique), password(BCrypt), name(실명), role
Role (enum)
  ADMIN, INSTRUCTOR, PROFESSOR, STUDENT

Lesson
  id, lessonDate(unique), title, description, githubUrl, important(boolean)
  resources: List<LessonResource>
  + 메서드: isMaterialReady()  # 리소스가 1개 이상이면 true (캘린더 초록 점)

LessonResource
  id, lesson, type, title, url, sortOrder
ResourceType (enum)
  ARTIFACT, GITHUB, ETC
  # v2: AI_SUMMARY, QUIZ, MINI_PROJECT 를 여기 추가해 확장

Notice
  id, category, title, content, author(Member), pinned(boolean), createdAt
NoticeCategory (enum, 한글 라벨 포함)
  ATTENDANCE("출석 관련"), EVALUATION("평가 관련"),
  CURRICULUM("커리큘럼"), CONTACT("연락처"), QNA("공지 Q&A")
```

모든 엔티티는 `BaseTimeEntity`를 상속한다.
상태 변경은 Setter 대신 엔티티 메서드로 한다 (예: `lesson.update(...)`, `lesson.addResource(...)`, `notice.pin()`).

---

## 5. URL

| 메서드   | URL                                              | 설명                                  | 권한              |
|----------|--------------------------------------------------|---------------------------------------|-------------------|
| GET      | `/login`                                         | 로그인                                | 전체              |
| GET      | `/`                                              | 홈 (`?month=2026-11&date=2026-11-26`) | 로그인            |
| GET      | `/lessons/{date}`                                | 수업 상세                             | 로그인            |
| GET      | `/notices?category=ATTENDANCE`                   | 공지 목록 (카테고리 없으면 전체)      | 로그인            |
| GET      | `/notices/{id}`                                  | 공지 상세                             | 로그인            |
| GET/POST | `/admin/notices/new`                             | 공지 작성                             | ADMIN, INSTRUCTOR |
| GET      | `/admin/lessons`                                 | 수업 관리 목록                        | ADMIN             |
| GET/POST | `/admin/lessons/new`, `/admin/lessons/{id}/edit` | 수업 등록·수정                        | ADMIN             |
| POST     | `/admin/lessons/{id}/resources`                  | 아티팩트 링크 추가                    | ADMIN             |
| GET      | `/coming-soon?feature=chat`                      | v2 준비 중 페이지                     | 로그인            |

---

## 6. 홈 화면 규칙

- **캘린더는 서버 렌더링**으로 만든다 (Thymeleaf 격자, `YearMonth` 기반). FullCalendar 등 외부 라이브러리 금지
- 월 이동: `?month=2026-12`
- 날짜 셀: 날짜 숫자 + 수업명 한 줄 (넘치면 말줄임 `…`, `title` 속성으로 전체 표시). 셀 높이 고정
- 표시 규칙
    - 초록 점: 자료 준비 완료 (`isMaterialReady()`)
    - 선택 날짜: 네이비 테두리 + 연한 블루 배경
    - 중요 일정 (`important=true`): 연분홍 배경
    - 일요일 숫자: 붉은 계열
- 날짜 클릭 → `?date=` 로 이동, 아래 상세 패널 갱신
- 상세 패널 버튼 6개: AI 수업 정리 / GitHub 코드 / 강사 아티팩트 / 복습 퀴즈 / 미니 프로젝트 / 수업 Q&A
    - MVP에서 동작: GitHub 코드, 강사 아티팩트
    - 나머지: 비활성 스타일 + "준비 중" 페이지로 연결
    - 자료가 없으면 해당 버튼도 비활성
- 하단 카드 2개 (번개 학습방, 프로젝트 전시관): MVP에서는 "곧 열려요" 카드

---

## 7. 디자인 토큰 (`tokens.css`)

```css
:root {
  --color-ivory: #F7F5EE;   /* 배경 35% */
  --color-white: #FFFFFF;   /* 카드 35% */
  --color-navy:  #34466A;   /* 강조·선택·로고 15% */
  --color-blue:  #9AAEC8;   /* 보조 7% */
  --color-green: #8FA68E;   /* 완료 상태 4% */
  --color-pink:  #E9C6D1;   /* 중요 공지·일정 4% */
  --color-text:  #2B2F38;
  --color-muted: #6B7280;
  --color-border:#E3E1DA;
  --radius-card: 12px;
  --radius-cell: 8px;
}
```

(색상 값은 와이어프레임 기준 근사치. 실제 값으로 교체 가능)

- 카드: 흰 배경, 얇은 테두리, 그림자 거의 없음
- 사이드바 메뉴는 줄바꿈되지 않게 너비 확보
- 색은 반드시 변수로만 사용. 하드코딩 금지

---

## 8. 시드 데이터 (`data.sql` 또는 `CommandLineRunner`)

- 계정: admin / instructor / student1~3 (비밀번호 `1234`, BCrypt 인코딩), 실명 포함
- 2026년 11월 수업: 와이어프레임 캘린더와 동일
    - 2 HTML 기초, 3 CSS 기초, 4 Flexbox, 5 Grid, 6 반응형 웹
    - 9 JavaScript, 10 배열과 객체, 11 DOM 조작, 12 이벤트, 13 비동기 처리
    - 16 React 기초, 17 컴포넌트, 18 Props, 19 State와 Hooks, 20 API 연동
    - 23 프로젝트 기획, 24~26 프로젝트 실습, 27 결과 발표 (important)
- 일부 수업에 아티팩트·GitHub 리소스를 넣어 초록 점이 보이게
- 필독 공지 1개: "프로젝트 평가 및 제출 안내" (pinned)

---

## 9. 개발 단계

각 단계가 끝나면 실행 확인 → 커밋 → 다음 단계. **한 단계에서 다음 단계 기능을 미리 만들지 않는다.**

| 단계 | 내용                                                                                           | 완료 기준                            |
|------|------------------------------------------------------------------------------------------------|--------------------------------------|
| 1    | 프로젝트 생성, 패키지 구조, 레이아웃(header/sidebar fragment), 디자인 토큰, coming-soon 페이지 | 모든 메뉴 클릭 시 페이지 이동        |
| 2    | Member, Spring Security 폼 로그인, 시드 계정, 헤더에 실명 표시                                 | 로그인 안 하면 `/login`으로 이동     |
| 3    | Lesson, LessonResource, 시드 수업, 홈 캘린더 + 상세 패널                                       | 와이어프레임과 같은 홈 화면          |
| 4    | Notice, 카테고리 필터, 필독 배너, 작성 화면                                                    | 사이드바 공지 메뉴 5개 동작          |
| 5    | 관리자 수업 등록·수정·리소스 추가                                                              | 관리자로 등록한 수업이 캘린더에 반영 |
| 6    | README (스크린샷, 구조, v2 로드맵)                                                             | —                                    |

---

## 10. 금지 규칙

- v2 기능을 미리 구현하지 않는다 (WebSocket, 외부 AI API, GitHub API, 스케줄러 금지)
- 회원가입 기능을 만들지 않는다
- Controller에 비즈니스 로직을 넣지 않는다 (Controller → Service → Repository)
- 엔티티를 템플릿에 직접 넘기지 않고, 화면용 데이터는 dto로 만든다
- 외부 CSS/JS 프레임워크를 추가하지 않는다
- 요청 범위 밖의 파일을 임의로 리팩터링하지 않는다

---

## 11. v2 확장 가이드 (참고용)

- **AI 정리·퀴즈**: `material` 패키지. `AiMaterialClient` 인터페이스를 두고 구현체를 교체 가능하게. 결과는 `LessonMaterial`(lesson, type, content
  JSON)로 저장
- **GitHub 동기화**: `sync` 패키지. `GitHubClient` 인터페이스 + 관리자 수동 버튼 → 이후 `@Scheduled` 또는 Webhook
- **번개 학습방**: `chat` 패키지. ChatRoom (주제/질문 제목, 방장, 공개), ChatMessage. WebSocket (STOMP)
- **전시관**: `showcase` 패키지. 작품명, 설명, GitHub 링크, 작성자, 연관 수업
- **Q&A**: `qna` 패키지. 수업 연결 (선택) 가능한 질문·답변
