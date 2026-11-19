# BootHub — 프로젝트 지침

부트캠프 수강생·강사·관리자를 위한 학습 허브.
**1일 MVP를 먼저 완성하고, 이후 기능을 같은 구조 위에 계속 쌓는다.**
AI 코딩 도구는 작업 전에 이 파일을 반드시 읽고, 여기 적힌 범위와 규칙을 지킨다.

---

## 1. 기술 스택

- Java 21, Spring Boot 4 (Spring Security 7, Hibernate 7)
- Spring Web, Thymeleaf, Spring Data JPA, Validation
- Spring Security (폼 로그인, BCrypt) — 2단계부터 적용
- H2 (file 모드: `jdbc:h2:file:./data/boothub`)
- Lombok
- 프론트: Thymeleaf + 순수 CSS (CSS 변수). 프레임워크·외부 JS 라이브러리 사용 금지
- 폰트: Pretendard (CDN 링크 1개만 예외로 허용)

### 설정 값과 비밀 값
- **외부 링크 (Slack, GitHub, Notion)**
  - URL을 템플릿이나 코드에 직접 쓰지 않는다
  - `application.yaml`에는 `boothub.links.slack` 같은 키만 두고, 실제 값은 환경변수
    (`BOOTHUB_LINKS_SLACK`, `BOOTHUB_LINKS_GITHUB`, `BOOTHUB_LINKS_NOTION`) 또는
    git에 올리지 않는 `application-local.yml`에서 읽는다
  - `application-local.yml`(`.yaml` 포함)은 `.gitignore`에 넣는다
- **관리자 계정**
  - 첫 ADMIN 계정은 `CommandLineRunner`로, 회원 테이블이 비어 있을 때만 생성한다
  - 관리자 아이디·비밀번호는 환경변수 `BOOTHUB_ADMIN_ID`, `BOOTHUB_ADMIN_PASSWORD`에서 읽는다.
    코드와 설정 파일에 비밀번호를 쓰지 않는다
  - 회원 테이블이 비어 있는데 환경변수가 없으면 앱 시작을 실패시키고 이유를 로그로 알린다
  - 연습용 계정(t3-01, s3-001~s3-003) 시드는 `local` 프로필에서만 생성한다
  - 아이디·비밀번호 형식은 4번 "계정 규칙"을 따른다

---

## 2. 범위

### MVP (지금 만든다)
| 기능 | 내용 |
|---|---|
| 공통 레이아웃 | 헤더(로고, Slack/GitHub/Notion 고정 링크), 좌측 사이드바, 디자인 토큰 |
| 로그인 | 회원가입 없음. 관리자가 발급한 계정만 로그인. 실명 표시 |
| 홈 | 필독 공지 배너, 월간 수업 캘린더, 선택 날짜 상세 패널 |
| 수업 | 날짜별 수업, GitHub 링크, 강사 아티팩트 링크 (링크 등록만, 파일 업로드 없음) |
| 공지 | 카테고리별 게시판 (출석/평가/커리큘럼/연락처/공지 Q&A), 필독 고정 |
| 관리자 | 수업 등록·수정, 아티팩트 링크 추가, 공지 작성, 회원 관리 (계정 생성·비밀번호 초기화·비활성화) |

### v2 (지금은 만들지 않는다 — 자리만 비워둔다)
- AI 수업 정리 (코드·주석 기반 표/구조 그림)
- 난이도별 복습 퀴즈
- 복습용 미니 프로젝트 주제
- 프로젝트 전시관
- 번개 학습방 (실시간 채팅, WebSocket)
- 학습 Q&A / 수업 Q&A
- 수업 종료 시 GitHub 자동 동기화
- 세부 열람 권한 (현재는 로그인 = 열람 허가)
- 파일 업로드 (MVP에서 아티팩트는 링크 등록만 한다)

v2 항목은 사이드바 메뉴와 버튼만 존재하고, 누르면 "준비 중" 페이지를 보여준다.

---

## 3. 패키지 구조 (도메인별)

```
com.wanted.boothub
├── global
│   ├── config          # SecurityConfig, JpaAuditingConfig, ClockConfig(한국 시간 기준 "오늘")
│   │                   # WebConfig — v2 예정 (아직 없음)
│   ├── common          # BaseTimeEntity (createdAt, updatedAt)
│   ├── exception       # BusinessException(규칙 위반 → 폼 오류), NotFoundException(@ResponseStatus 404)
│   │                   # GlobalExceptionHandler — v2 예정 (아직 없음. 지금은 Spring Boot 기본 /error + templates/error)
│   └── web             # GlobalModelAdvice(외부 링크·로그인 사용자·현재 경로), ExternalLinkProperties,
│                       # ComingSoonController, ComingSoonFeature, ComingSoonView (v2 "준비 중" 페이지)
├── member
│   ├── Member, Role
│   ├── MemberRepository
│   ├── MemberService
│   ├── MemberSeeder         # 첫 ADMIN(환경변수) + local 연습용 계정
│   ├── AdminAccountProperties  # BOOTHUB_ADMIN_ID / BOOTHUB_ADMIN_PASSWORD 바인딩
│   ├── LoginController      # /login 화면
│   ├── LoginUser, LoginUserDetailsService
│   ├── PasswordGenerator    # 10자 랜덤 비밀번호 생성 (계정 생성·초기화)
│   ├── PasswordPolicy       # 사용자 비밀번호 검증 규칙
│   ├── PasswordChangeRequiredFilter, LoginSuccessHandler  # mustChangePassword 강제 이동
│   ├── AccountController    # /account/password (본인 비밀번호 변경)
│   ├── MemberSessionExpirer # 비활성화·비밀번호 초기화한 회원의 로그인 세션 끊기 (SessionRegistry)
│   └── dto                  # LoginUserDto, MemberSummaryDto, MemberForm, PasswordChangeForm, IssuedPasswordDto
├── lesson
│   ├── Lesson, LessonResource, ResourceType
│   ├── LessonRepository, LessonResourceRepository
│   ├── LessonService
│   ├── LessonSeeder         # 2026년 11월 시드 수업
│   ├── CalendarService      # 월간 캘린더 데이터 생성
│   ├── dto                  # CalendarMonthDto, CalendarDayDto, LessonDetailDto, AdminLessonRowDto,
│   │                        # LessonForm, LessonResourceForm
│   └── LessonController
├── notice
│   ├── Notice, NoticeCategory
│   ├── NoticeRepository
│   ├── NoticeService
│   ├── NoticeSeeder         # 시드 공지 (필독 1개 포함)
│   ├── dto                  # NoticeSummaryDto, NoticeDetailDto, NoticeBannerDto, NoticeForm
│   └── NoticeController
├── admin
│   └── AdminController      # 관리자 화면 전용, 로직은 각 도메인 Service 호출
└── home
    └── HomeController
```

v2에서 추가될 패키지: `material`(AI 정리·퀴즈), `showcase`, `chat`, `qna`, `sync`.
**새 기능은 새 패키지로 추가하고, 기존 도메인 코드는 최소한만 건드린다.**

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
├── admin/lessons.html, lesson-form.html, members.html, member-form.html
├── auth/login.html
├── account/password.html    # 비밀번호 변경 (mustChangePassword 강제 이동 대상)
├── common/coming-soon.html  # v2 기능 공용 "준비 중"
└── error/                   # Spring Boot 기본 오류 처리(/error)가 상태 코드로 고르는 화면 (모두 공통 레이아웃)
    ├── 403.html, 404.html, 500.html
    └── 4xx.html, 5xx.html   # 그 밖의 4xx(400 등) / 5xx(502·503 등)
static/css/
├── tokens.css               # 디자인 토큰만
└── app.css                  # 컴포넌트 스타일
```

---

## 4. 도메인 모델

```
Member
  id, loginId(unique, 변경 불가), password(BCrypt), name(실명), role,
  active(boolean, 비활성 계정은 로그인 불가),
  mustChangePassword(boolean, true면 로그인 직후 비밀번호 변경 강제)
  + 메서드: resetPassword(encodedPassword)   # 관리자 초기화 → mustChangePassword = true
           changePassword(encodedPassword)  # 본인 변경 → mustChangePassword = false
           deactivate()
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

### 계정 규칙

**아이디 (`loginId`)**
- 형식: `역할-기수-번호`
  | 역할 | 형식 | 예 |
  |---|---|---|
  | STUDENT | `s{기수}-{번호 3자리}` | `s3-017` |
  | INSTRUCTOR | `t{기수}-{번호 2자리}` | `t3-01` |
  | PROFESSOR | `p{기수}-{번호 2자리}` | `p3-01` |
  | ADMIN | 고정 이름 | `admin` |
- 영문 소문자·숫자·하이픈만 허용, 4~20자 (`^[a-z0-9-]{4,20}$`), 중복 불가
- 생성 후 변경 불가: 아이디를 바꾸는 엔티티 메서드·화면을 만들지 않는다 (`@Column(updatable = false)`)
- 환경변수로 만드는 첫 ADMIN 아이디도 같은 규칙을 지켜야 한다

**비밀번호 — 시스템 발급 (계정 생성·초기화)**
- 관리자가 입력하지 않는다. 시스템이 10자 랜덤 비밀번호(영문 대소문자+숫자, 영문·숫자 각 1자 이상)를 `SecureRandom`으로 생성한다
- 생성 직후 관리자 화면에 **한 번만** 표시한다 (flash attribute로 전달). 로그·세션·DB에 평문을 남기지 않는다
- DB에는 BCrypt 해시만 저장한다
- 발급·초기화된 계정은 `mustChangePassword = true`

**비밀번호 — 사용자 변경 (`/account/password`)**
- 8자 이상, 영문과 숫자를 모두 포함
- 아이디와 같거나 아이디를 포함하면 거부 (대소문자 무시)
- 현재 비밀번호 확인 후 변경, 변경되면 `mustChangePassword = false`

**변경 강제 흐름**
- `mustChangePassword = true`인 사용자는 로그인 성공 직후 `/account/password`로 이동한다
- 변경 전에는 `/account/password`, `/logout`, 정적 리소스 외 모든 요청을 `/account/password`로 리다이렉트한다

**예외**
- 첫 ADMIN(환경변수 비밀번호)과 `local` 연습용 계정은 `mustChangePassword = false`로 생성한다

---

## 5. URL

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| GET | `/login` | 로그인 | 전체 |
| GET/POST | `/account/password` | 본인 비밀번호 변경 (`mustChangePassword`면 강제 이동) | 로그인 |
| GET | `/` | 홈 (`?month=2026-11&date=2026-11-26`) | 로그인 |
| GET | `/lessons/{date}` | 수업 상세 | 로그인 |
| GET | `/notices?category=ATTENDANCE` | 공지 목록 (카테고리 없으면 전체) | 로그인 |
| GET | `/notices/{id}` | 공지 상세 | 로그인 |
| GET/POST | `/admin/notices/new` | 공지 작성 | ADMIN, INSTRUCTOR |
| GET | `/admin/lessons` | 수업 관리 목록 | ADMIN |
| GET/POST | `/admin/lessons/new`, `/admin/lessons/{id}/edit` | 수업 등록·수정 | ADMIN |
| POST | `/admin/lessons/{id}/resources` | 아티팩트 링크 추가 (링크만, 파일 업로드 없음) | ADMIN |
| GET | `/admin/members` | 회원 목록 (비밀번호 초기화·비활성화 버튼 포함) | ADMIN |
| GET/POST | `/admin/members/new` | 계정 생성 (아이디, 실명, 역할. 비밀번호는 시스템 랜덤 발급 후 한 번만 표시) | ADMIN |
| POST | `/admin/members/{id}/reset-password` | 비밀번호 초기화 (랜덤 재발급, 한 번만 표시) | ADMIN |
| POST | `/admin/members/{id}/deactivate` | 계정 비활성화 (본인 계정은 불가) | ADMIN |
| GET | `/coming-soon?feature=chat` | v2 준비 중 페이지 | 로그인 |

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
  --color-ivory: #F8F7F4;   /* 배경 35% */
  --color-white: #FFFFFF;   /* 카드 35% */
  --color-navy:  #29466B;   /* 메인·선택·로고 15% */
  --color-blue:  #91ABC9;   /* 보조 7% */
  --color-green: #83A890;   /* 완료 상태 4% */
  --color-pink:  #F4C6D7;   /* 강조·중요 일정 4% */
  --color-sunday: #C2566B;  /* 캘린더 일요일 숫자 (붉은 계열) */
  /* 파생 토큰: 연한 배경 (선택 날짜·배지 / 중요 일정·필독 배너) */
  --color-blue-soft: color-mix(in srgb, var(--color-blue) 22%, var(--color-white));
  --color-pink-soft: color-mix(in srgb, var(--color-pink) 35%, var(--color-white));
  --font-base: 'Pretendard', -apple-system, 'Malgun Gothic', sans-serif;
  --color-text:  #2B2F38;
  --color-muted: #6B7280;
  --color-border:#E3E1DA;
  --radius-card: 12px;
  --radius-cell: 8px;
}
```

- 카드: 흰 배경, 얇은 테두리, 그림자 거의 없음
- 사이드바 메뉴는 줄바꿈되지 않게 너비 확보
- 색은 반드시 변수로만 사용. 하드코딩 금지
- 연한 배경색이 필요하면 컴포넌트에서 `color-mix`를 새로 쓰지 말고 파생 토큰(`-soft`)을 쓴다

---

## 8. 시드 데이터 (`CommandLineRunner`, 해당 테이블이 비어 있을 때만 삽입)

- 계정 (자세한 규칙은 1번 "설정 값과 비밀 값")
  - 첫 ADMIN: 환경변수 `BOOTHUB_ADMIN_ID`, `BOOTHUB_ADMIN_PASSWORD`로 생성 (모든 프로필)
  - 연습용 강사 `t3-01` / 학생 `s3-001`~`s3-003`: `local` 프로필에서만 생성 (비밀번호 `1234`, BCrypt 인코딩,
    `mustChangePassword = false`), 실명 포함. 고정 비밀번호는 `local` 연습용에만 허용하는 예외
- 2026년 11월 수업: 와이어프레임 캘린더와 동일
  - 2 HTML 기초, 3 CSS 기초, 4 Flexbox, 5 Grid, 6 반응형 웹
  - 9 JavaScript, 10 배열과 객체, 11 DOM 조작, 12 이벤트, 13 비동기 처리
  - 16 React 기초, 17 컴포넌트, 18 Props, 19 State와 Hooks, 20 API 연동
  - 23 프로젝트 기획, 24~26 프로젝트 실습, 27 결과 발표(important)
- 일부 수업에 아티팩트·GitHub 리소스를 넣어 초록 점이 보이게
- 필독 공지 1개: "프로젝트 평가 및 제출 안내" (pinned)

---

## 9. 개발 단계

각 단계가 끝나면 실행 확인 → 커밋 → 다음 단계. **한 단계에서 다음 단계 기능을 미리 만들지 않는다.**

| 단계 | 내용 | 완료 기준 |
|---|---|---|
| 1 | 프로젝트 생성, 패키지 구조, 레이아웃(header/sidebar fragment), 디자인 토큰, coming-soon 페이지 | 모든 메뉴 클릭 시 페이지 이동 |
| 2 | Member, Spring Security 폼 로그인, 시드 계정, 헤더에 실명 표시, `mustChangePassword` 강제 변경 흐름(`/account/password`) | 로그인 안 하면 `/login`으로 이동, `mustChangePassword` 계정은 변경 전까지 `/account/password`로 이동 |
| 3 | Lesson, LessonResource, 시드 수업, 홈 캘린더 + 상세 패널 | 와이어프레임과 같은 홈 화면 |
| 4 | Notice, 카테고리 필터, 필독 배너, 작성 화면 | 사이드바 공지 메뉴 5개 동작 |
| 5 | 관리자 수업 등록·수정·리소스(링크) 추가, 회원 관리 (계정 생성·비밀번호 초기화 시 랜덤 비밀번호 발급·1회 표시, 비활성화) | 관리자로 등록한 수업이 캘린더에 반영, 관리자가 만든 계정으로 로그인 → 비밀번호 변경 강제, 비활성 계정은 로그인 불가 |
| 6 | README (스크린샷, 구조, v2 로드맵) | — |

---

## 10. 금지 규칙

- v2 기능을 미리 구현하지 않는다 (WebSocket, 외부 AI API, GitHub API, 스케줄러 금지)
- 회원가입 기능을 만들지 않는다
- Controller에 비즈니스 로직을 넣지 않는다 (Controller → Service → Repository)
- 엔티티를 템플릿에 직접 넘기지 않고, 화면용 데이터는 dto로 만든다
- 외부 CSS/JS 프레임워크를 추가하지 않는다 (Pretendard 폰트 CDN만 예외)
- Spring Boot 3 이하 방식(WebSecurityConfigurerAdapter, and() 체이닝 등)을 사용하지 않는다
- 외부 URL·비밀번호 같은 환경별 값과 비밀 값을 코드·템플릿·커밋되는 설정 파일에 쓰지 않는다
- 파일 업로드 기능을 만들지 않는다 (v2). 아티팩트는 링크 등록만 한다
- 요청 범위 밖의 파일을 임의로 리팩터링하지 않는다

---

## 11. v2 확장 가이드 (참고용)

상세 설계는 `docs/ai-module-spec.md` 참고. 단, Java 프로젝트이므로 응답 검증은 Zod 대신 Jackson + Bean Validation, Mermaid는 v2에서만 mermaid.js 예외 허용.


- **AI 정리·퀴즈**: `material` 패키지. `AiMaterialClient` 인터페이스를 두고 구현체를 교체 가능하게. 결과는 `LessonMaterial`(lesson, type, content JSON)로 저장
- **GitHub 동기화**: `sync` 패키지. `GitHubClient` 인터페이스 + 관리자 수동 버튼 → 이후 `@Scheduled` 또는 Webhook
- **번개 학습방**: `chat` 패키지. ChatRoom(주제/질문 제목, 방장, 공개), ChatMessage. WebSocket(STOMP)
- **전시관**: `showcase` 패키지. 작품명, 설명, GitHub 링크, 작성자, 연관 수업
- **Q&A**: `qna` 패키지. 수업 연결(선택) 가능한 질문·답변
