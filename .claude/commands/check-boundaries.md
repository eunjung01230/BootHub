---
description: 층 경계와 도메인 간 얽힘을 측정하고 ArchUnit 규칙으로 검증한다
argument-hint: "[기준 패키지, 예: com.example.lms] [baseline|check]"
---

# 경계 검증: $ARGUMENTS

이 프로젝트(Spring Boot)의 **바꾸는 비용**을 검증한다. 실행 결과가 아니라 경계를 본다.
층 역할 기준은 아래와 같다.

- Controller: 요청과 화면 이동만. Repository·엔티티 규칙을 직접 다루지 않는다.
- Service: 유스케이스 조립. 다른 도메인의 Repository를 직접 주입받지 않는다.
- 엔티티·Policy: 규칙. DTO, Controller, Spring 웹 계층을 모른다.
- DTO: 옮기는 값. 로직을 갖지 않는다.
- Repository: 저장만.

첫 번째 인자가 없으면 `src/main/java`에서 `@SpringBootApplication` 클래스의 패키지를 기준 패키지로 쓴다.
두 번째 인자가 `baseline`이면 측정값을 `docs/boundary-baseline.json`에 저장하고, `check`이면 그 파일과 비교한다. 없으면 측정만 한다.

## 1단계: 구조 파악

1. 기준 패키지 아래 최상위 패키지를 도메인 후보로 나열한다.
2. 패키지가 도메인별(`member/controller`)인지 층별(`controller/member`)인지 판단하고 사용자에게 한 줄로 알린다.
3. `build.gradle` 또는 `pom.xml`에서 Spring Boot 버전과 테스트 의존성을 확인한다.

코드를 고치기 전에 이 결과를 보여주고, 도메인 분류가 애매한 패키지(common, global, util 등)를 어떻게 취급할지 확인받는다.

## 2단계: 정량 측정

`scripts/boundary_metrics.py`를 만들어(이미 있으면 재사용) Python 3.8+ 표준 라이브러리만으로 다음을 센다. Windows 경로에서도 동작해야 한다.

- 본 코드 클래스 수와 줄 수 (`src/main/java` 기준, 테스트 제외)
- 도메인 간 import 줄 수, 그리고 도메인 쌍별 상위 10개
- Service 생성자 주입 수와, 다른 도메인 Service·Repository를 주입받는 곳
- 생성자 파라미터가 5개 이상인 클래스 목록
- 도메인 간 순환 참조 쌍
- git 이력 기준 커밋당 수정된 도메인 수의 평균, 함께 자주 바뀐 도메인 쌍 상위 10개 (`git log --name-only`, 머지 커밋 제외)

## 3단계: ArchUnit 규칙 추가

1. 테스트 의존성에 `com.tngtech.archunit:archunit-junit5`가 없으면 추가한다(버전은 현재 안정 버전으로 고정).
2. `src/test/java/<기준패키지>/architecture/` 아래에 다음 규칙을 작성한다. 패키지 구조(도메인별/층별)에 맞게 패턴을 조정한다.
   - 층 규칙: Controller → Service → Repository 방향만 허용
   - 엔티티는 DTO·Controller 패키지에 의존하지 않음
   - Service는 다른 도메인의 Repository에 의존하지 않음
   - Policy 클래스는 `org.springframework..`에 의존하지 않음
   - 도메인 슬라이스 간 순환 금지 (`slices().matching(...).should().beFreeOfCycles()`)
3. Spring Boot 3 이상이면 Spring Modulith `ApplicationModules.of(...).verify()` 테스트도 제안만 하고, 사용자가 원할 때만 추가한다.

## 4단계: 실행

ArchUnit 테스트만 골라 실행한다 (`gradlew test --tests "*architecture*"` 또는 Maven 대응 명령).
규칙 위반을 규칙별로 개수와 대표 사례 3개씩 정리한다. **위반을 고치려고 본 코드를 수정하지 않는다.** 이 명령은 측정과 검증만 한다.

## 5단계: 보고

다음 형식으로 한국어로 보고한다.

1. 측정 요약: 클래스 수, 도메인 수, 도메인 간 import, Service 주입, 순환 쌍, 커밋당 평균 도메인 수
2. `check` 모드면 baseline 대비 증감
3. 가장 많이 얽힌 클래스 상위 5개와, 각 클래스를 직접 읽고 판단한 이유 한두 문장
4. 규칙 위반 요약
5. 가장 먼저 풀면 효과가 큰 얽힘 3개와 방향 제안 (코드 수정은 하지 않음)
