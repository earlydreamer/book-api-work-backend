# Today Us Backend Contract Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `today-us-front`가 읽는 상태 모델을 기준으로 백엔드 계약 문서와 Spring Boot stub 서버 뼈대를 맞춘다.

**Architecture:** 프론트 친화적인 BFF 응답을 유지한 채, `controller -> service -> support.fixture` 구조로 먼저 고정한다. DB/JPA/Flyway는 다음 슬라이스로 미루고, 이번 라운드는 DTO, validation, ProblemDetail, OpenAPI, auth toggle, contract endpoint를 실행 가능한 서버 형태로 잠근다.

> 2026-04-07 구현 메모: 초안은 stub-only 범위로 시작했지만, 현재 코드는 이 계획을 확장해서 `entity/repository/Flyway/H2` core slice까지 반영한 상태다.

**Tech Stack:** Spring Boot 4.0.x, Java 21, Gradle Kotlin DSL, Spring MVC, Validation, Security, ProblemDetail, springdoc-openapi

---

## Step 0. Scope Challenge

### 지금 이미 있는 것

| 하위 문제 | 이미 있는 코드 / 문서 | 재사용 방식 |
|---|---|---|
| 현재/이전 관계 정책 | `today-us-front/src/context/sessionState.ts` | DTO shape와 relation lifecycle 규칙으로 옮긴다 |
| 책 진행도 계산 규칙 | `today-us-front/src/mock/selectors.ts` | `bookProgress` 응답 필드 이름과 기준을 그대로 유지한다 |
| 보관함 section UX | `today-us-front/src/pages/Archive.tsx` | `GET /api/v1/archive` sectioned response로 맞춘다 |
| 연결하기 플로우 | `today-us-front/src/pages/Connect.tsx` | create / preview / accept invite 3 endpoint로 분리한다 |
| 기록 입력 플로우 | `today-us-front/src/pages/Record.tsx` | `PUT /api/v1/day-cards/{localDate}/entry` request/response로 옮긴다 |
| 큰 그림 아키텍처 | `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md` | runtime 선택과 다음 슬라이스 경계를 유지한다 |

### 최소 변경 셋

1. 공통 계약 문서 보강
2. 백엔드 계획 문서 작성
3. Spring Boot skeleton 생성
4. DTO + controller + stub service 추가
5. contract smoke test 추가

### 복잡도 체크

- 문서 수정과 스캐폴딩을 합치면 8파일을 넘는다.
- 하지만 실제 행위는 `문서 동기화`, `build skeleton`, `contract endpoint` 세 덩어리뿐이다.
- 새 service abstraction을 남발하지 않고 `contract service + stub fixture`로 한 번만 묶으면 과설계는 피할 수 있다.

### Search check

- 2026-04-07 기준 `https://start.spring.io` 공식 metadata는 Spring Boot `4.0.5`를 현재 안정 release로 노출한다.
- 따라서 blueprint의 `4.0.3` 표기는 패치 기준으로는 stale일 가능성이 높다.
- 이번 스캐폴딩은 `4.0.5` 기준으로 생성하고, 문서에서는 `4.0.x`와 실제 scaffold 버전을 함께 적는다.

### TODOS 교차 확인

- 이번 작업을 막는 TODO는 없다.
- 대신 `운영 자동화 심화`, `디지털 미리보기 강화`는 snapshot/order 실제 구현 뒤로 남기는 게 맞다.

### Completeness check

- shortcut은 문서만 고치고 코드는 안 만드는 거다. 그건 다음 세션이 다시 해석 비용을 낸다.
- complete option은 실행 가능한 stub 서버까지 남기는 거다.
- 이번 라운드는 lake 범위라서 complete option으로 간다.

---

## 아키텍처 다이어그램

```text
today-us-front
  ├─ Feed.tsx
  │   └─ GET /api/v1/me/home
  ├─ Archive.tsx
  │   └─ GET /api/v1/archive
  ├─ Connect.tsx
  │   ├─ POST /api/v1/couples/invites
  │   ├─ GET /api/v1/couples/invites/{inviteCode}
  │   └─ POST /api/v1/couples/invites/{inviteCode}/accept
  ├─ Record.tsx
  │   ├─ GET /api/v1/day-cards/today
  │   └─ PUT /api/v1/day-cards/{localDate}/entry
  ├─ MakeBook.tsx
  │   └─ GET /api/v1/book-snapshots/current
  └─ Me.tsx
      └─ POST /api/v1/couples/current/unlink

Spring Boot contract scaffold
  ├─ controller
  ├─ dto
  ├─ contract stub service
  ├─ validation + ProblemDetail
  ├─ auth toggle
  └─ OpenAPI
```

---

## Decision Audit Trail

| # | Phase | Decision | Classification | Principle | Rationale | Rejected |
|---|---|---|---|---|---|---|
| 1 | CEO | 홈/보관함/책 응답을 BFF 스타일로 유지 | mechanical | completeness | 프론트가 이미 화면 중심 상태를 읽고 있어서 adapter 복잡도를 줄인다 | resource 조합형 |
| 2 | CEO | reconnect 시 새 `couple_id` 유지 | mechanical | explicit | 현재 관계와 이전 기록을 가장 덜 헷갈리게 분리한다 | 기존 row 재오픈 |
| 3 | Eng | 이번 라운드는 DB 없이 stub server까지 구현 | mechanical | boil lakes | 문서만 남기면 다음 세션에서 다시 해석한다 | 문서-only handoff |
| 4 | Eng | Gradle Kotlin DSL + Spring Boot 4.0.5 scaffold 사용 | taste | pragmatic | 공식 Initializr 현재 release를 쓰는 쪽이 설정 drift가 적다 | 수동 초기화, Maven |
| 5 | Eng | security auth toggle 도입 | taste | reversibility | local stub과 실제 JWT 검증 전환 비용을 낮춘다 | 전면 permitAll, 전면 auth 강제 |

---

## Architecture Review

### 검토 결과

1. **[P1] (confidence: 9/10) 문서만 있고 실행 가능한 stub이 없으면 계약 drift가 바로 생긴다**
   프론트는 이미 구체적인 상태 분기를 쓰고 있는데, 백엔드가 문서-only 상태면 `coupleId`, `inviteCode`, `bookProgress.state` 같은 필드가 다음 구현에서 쉽게 바뀐다.
   추천: controller + dto + stub response까지 한 번에 만든다.

2. **[P2] (confidence: 8/10) `GET /api/v1/couples/invites/{inviteCode}`가 없으면 현재 연결하기 UX를 자연스럽게 못 붙인다**
   지금 `Connect.tsx`는 코드 입력 뒤 파트너 이름 미리보기를 보여준다.
   accept만 두면 프론트는 검증과 미리보기를 동시에 처리해야 해서 UX 분기가 지저분해진다.

3. **[P2] (confidence: 8/10) DB auto-config를 바로 켜면 empty repo 단계에서 시작조차 못 할 수 있다**
   JPA/Flyway/Postgres를 dependency에 넣는 건 맞지만, datasource 없이 앱이 뜨지 않으면 scaffold 가치가 떨어진다.
   추천: 이번 슬라이스는 DB auto-config를 명시적으로 끄고, 다음 persistence 슬라이스에서 다시 연다.

### 섹션 결론

- scope reduction은 하지 않는다.
- 대신 persistence와 upload/order를 분리해 blast radius를 통제한다.

---

## Code Quality Review

### 검토 결과

1. **[P2] (confidence: 8/10) DTO와 stub fixture가 섞이면 다음 슬라이스에서 파일이 바로 비대해진다**
   응답 record 정의와 예시 데이터 생성은 패키지는 가까워도 클래스로는 분리하는 게 낫다.

2. **[P2] (confidence: 7/10) enum naming을 Java 기본 대문자 그대로 JSON에 노출하면 프론트 adapter가 불필요하게 늘어난다**
   프론트가 이미 kebab-case / lower-case state를 기준으로 돌고 있어서 JSON serialization 레벨에서 맞춰주는 편이 낫다.

3. **[P3] (confidence: 7/10) README와 handoff 문서가 계속 "백엔드는 비어 있다"로 남아 있으면 다음 세션을 오도한다**
   코드보다 문서 drift가 먼저 사고를 만든다.

### 섹션 결론

- DTO는 feature package별 record + 공통 nested dto로 정리한다.
- stub data는 별도 fixture 클래스로 뺀다.
- README / handoff는 이번 diff에 같이 고친다.

---

## Test Review

### 적용할 테스트 전략

- WebMvc/MockMvc 수준에서 contract endpoint JSON shape를 고정한다.
- domain/persistence 테스트는 이번 라운드 범위 밖이다.
- regression 성격이 강한 건 `home`과 `archive`의 separation rules다.

### CODE PATH COVERAGE

```text
CODE PATH COVERAGE
===========================
[+] GET /api/v1/me/home
    ├── [GAP] connected 응답 shape 검증
    ├── [GAP] archived 필드 미포함 확인
    └── [GAP] relationship/bookProgress state 값 검증

[+] GET /api/v1/archive
    ├── [GAP] current / archived section 분리 검증
    ├── [GAP] archived recordLabel 존재 검증
    └── [GAP] current partnerName 누수 방지 검증

[+] POST /api/v1/couples/invites
    ├── [GAP] invite-pending 상태 검증
    └── [GAP] 잘못된 startDate validation 검증

[+] GET /api/v1/couples/invites/{inviteCode}
    ├── [GAP] invite preview 반환 검증
    └── [GAP] 미존재 코드 404 ProblemDetail 검증

[+] POST /api/v1/couples/invites/{inviteCode}/accept
    └── [GAP] connected 응답 + recordedDays 0 검증

[+] POST /api/v1/couples/current/unlink
    └── [GAP] unconnected 응답 + archiveUpdated 검증

[+] GET /api/v1/day-cards/today
    └── [GAP] todayCard state / me entry shape 검증

[+] PUT /api/v1/day-cards/{localDate}/entry
    ├── [GAP] emotionCode 필수 validation 검증
    └── [GAP] 요청 memo가 응답 todayCard에 반영되는지 검증

[+] GET /api/v1/book-snapshots/current
    ├── [GAP] candidateMoments 응답 shape 검증
    └── [GAP] snapshot/order null shape 검증

─────────────────────────────────
COVERAGE: 0/17 paths tested (0%)
  Code paths: 0/17
QUALITY: ★★★: 0  ★★: 0  ★: 0
GAPS: 17 paths need tests
─────────────────────────────────
```

### 테스트 계획 artifact

- 파일: `~/.gstack/projects/book-api-work-backend/<user>-feature-backend-contract-handoff-eng-review-test-plan-<timestamp>.md`
- 내용:
  - `/api/v1/me/home` connected 응답
  - `/api/v1/archive` current/archived 분리
  - invite create / preview / accept 흐름
  - unlink 응답
  - today card save validation
  - book current summary 응답

### 섹션 결론

- contract smoke test는 반드시 이번 라운드에 같이 넣는다.
- 회귀 테스트 우선순위는 `home`, `archive`, `day-card entry validation`이다.

---

## Failure Modes Registry

| Codepath | 현실적인 실패 방식 | 테스트 존재 여부 | 에러 처리 존재 여부 | 사용자 체감 |
|---|---|---|---|---|
| `/me/home` | archived 기록이 홈 응답에 섞임 | 계획상 추가 | 필요 | 피드에 이전 관계 기록이 섞여 UX 붕괴 |
| `/archive` | archived 라벨 누락 | 계획상 추가 | 필요 | 현재/이전 기록 구분 불가 |
| invite preview | 없는 코드도 성공 처리 | 계획상 추가 | 필요 | 잘못된 상대 이름으로 연결 시도 |
| unlink | 현재 관계 없는 상태에서 성공 처리 | 계획상 추가 | 필요 | 상태 전환 일관성 깨짐 |
| day-card save | 감정 없이 저장 | 계획상 추가 | 필요 | 제품 핵심 입력 규칙 깨짐 |
| book current | archived candidate가 섞임 | 계획상 추가 | 필요 | 책 진행도가 재연결 후 초기화되지 않음 |

**critical gap 판정**

- 현재는 모든 항목이 `무테스트 + 미구현`이라 critical gap 후보였다.
- 이번 라운드에서 contract smoke test와 ProblemDetail을 같이 넣으면 critical gap에서 빠진다.

---

## Performance Review

이번 스캐폴딩은 stub fixture 기반이라 성능 병목이 핵심 이슈는 아니다.

그래도 미리 적어둘 건 있다.

1. archive 응답은 다음 슬라이스에서 pagination 또는 date range가 필요하다.
2. home/book 응답에서 동일한 기록 계산을 각각 다시 하지 않게 service 계층에서 조합 포인트를 분리해야 한다.
3. 실제 JPA 전환 시 `ACTIVE couple` 조회와 `최근 30일 recorded day count`는 인덱스 없으면 바로 느려진다.

---

## NOT in Scope

- 실제 Supabase JWT 연동
- 실제 Postgres datasource 연결
- Flyway migration 파일 작성
- R2 presigned upload API
- snapshot create/build/order write path
- admin/ops surface

---

## Worktree Parallelization Strategy

| Step | Modules touched | Depends on |
|---|---|---|
| 문서 계약 정리 | `docs/specs/`, `docs/plans/`, `README.md` | — |
| Spring skeleton 생성 | repo root, `src/main/resources/`, build files | 문서 계약 정리 |
| contract endpoint 구현 | `src/main/java/dev/earlydreamer/todayus/controller/**`, `dto/**`, `service/**`, `support/**` | Spring skeleton 생성 |
| contract smoke test 구현 | `src/test/java/dev/earlydreamer/todayus/**` | contract endpoint 구현 |
| handoff 문서 동기화 | `docs/specs/`, `README.md` | contract endpoint 구현 |

### Parallel lanes

- Lane A: 문서 계약 정리 → Spring skeleton 생성
- Lane B: contract endpoint 구현 → contract smoke test 구현
- Lane C: handoff 문서 동기화

### 실행 순서

- A를 먼저 끝낸다.
- 그다음 B를 진행한다.
- C는 B 결과를 보고 마지막에 맞춘다.

### Conflict flags

- 문서와 code가 모두 `README.md`, `docs/specs/`를 건드리므로 실제론 순차 구현이 더 안전하다.
- 결론: 이번 라운드는 sequential implementation이 낫다.

---

## Completion Summary

- Step 0: Scope Challenge — complete option 유지
- Architecture Review: 3 issues found
- Code Quality Review: 3 issues found
- Test Review: diagram produced, contract smoke test 9건 구현 완료
- Performance Review: 3 issues found
- NOT in scope: written
- What already exists: written
- TODOS.md updates: 0 items proposed
- Failure modes: 0 critical gaps
- Outside voice: skipped
- Parallelization: sequential implementation 권장
- Lake Score: 5/5 recommendations chose complete option
- Verification: `./gradlew test`, `./gradlew build` 통과

---

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/plan-ceo-review` | Scope & strategy | 0 | — | — |
| Codex Review | `/codex review` | Independent 2nd opinion | 0 | — | — |
| Eng Review | `/plan-eng-review` | Architecture & tests (required) | 1 | clear | BFF contract 문서, Spring Boot stub scaffold, contract smoke test 9건 완료 |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | — | — |

**VERDICT:** ENG CLEARED, 실제 persistence와 auth slice를 붙일 준비가 됐다.
