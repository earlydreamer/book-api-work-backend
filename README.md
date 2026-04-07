# 오늘 우리 백엔드

`backend/book-api-work-backend`는 현재 백엔드 구현 대상 디렉터리다.

2026-04-07 기준으로 이 repo에는 아직 애플리케이션 코드가 없다.
지금 단계의 핵심은 코드를 빨리 쓰는 게 아니라, 프론트에서 잠근 상태 계약을 backend API와 데이터 모델로 정확히 옮기는 것이다.

---

## 1. 먼저 읽을 문서

1. `AGENTS.md`
2. `docs/README.md`
3. `docs/specs/2026-04-07-cross-env-handoff-v1.md`
4. `docs/specs/2026-04-05-today-our-mvp-v2.md`
5. `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`
6. `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
7. `docs/specs/2026-04-07-today-us-front-progress-v1.md`

---

## 2. 현재 상태

- Spring Boot 프로젝트 초기 세팅 전
- 엔드포인트, DTO, 관계 전환 정책은 문서로만 정리된 상태
- frontend는 이미 `현재 관계 / 이전 관계` 분리 정책을 기준으로 구현됨
- backend는 그 정책을 그대로 수용해야 함

---

## 3. 현재 잠긴 핵심 정책

### 관계 기록

- 연결을 끊어도 이전 기록은 삭제하지 않음
- 이전 기록은 archive/history로 유지
- 다시 연결하면 기록 수와 책 진행도는 `0일부터 다시 시작`
- 피드는 현재 관계만 보여줌
- 보관함은 현재 관계 / 이전 관계를 구분해서 보여줌

### 책 자격

- 최근 30일 중 기록 20일 이상

### backend 해석 초안

- `couples`는 영구 pair identity가 아니라 `관계 인스턴스`로 본다
- reconnect 시 기존 `couple_id` 재사용이 아니라 새 `couple_id` 생성

이 해석은 현재 backend 계약 초안의 핵심 가정이다.
구현 전에 다시 한 번 확인하고 들어가는 게 좋다.

---

## 4. 목표 스택

- `Spring Boot 4.0.3`
- `Java 21`
- `Spring Security`
- `Supabase Auth + Postgres`
- `Flyway`
- `Cloudflare R2`
- `WebClient`
- `DB-backed jobs`

세부 근거는 실행 청사진 문서를 따른다.

---

## 5. 바로 시작할 때의 추천 순서

1. Gradle 또는 Maven 기반 Spring Boot skeleton 생성
2. 인증/인가 경계와 `users`, `couples`, `day_cards` 계약 타입 고정
3. `GET /api/v1/me/home`, `GET /api/v1/archive`, `POST /api/v1/couples/current/unlink` DTO 먼저 확정
4. 그다음 day card / snapshot / order로 확장

핵심은 `order`보다 먼저 `relationship state`를 안정적으로 표현하는 거다.

---

## 6. 아직 열린 결정

1. reconnect 시 새 `couple_id` 생성 가정을 최종 확정할지
2. `GET /api/v1/me/home`를 BFF 응답으로 둘지, resource 조합형으로 둘지
3. archive 응답을 sectioned view model로 줄지, raw resource로 줄지
4. admin과 public을 실제로 어느 시점에 분리할지

---

## 7. 주의

- 이 repo는 독립 nested git repo다.
- 공통 문서와 프론트 참고 문서는 이 repo의 `docs/` 아래에 로컬 사본으로 보관한다.
- 프론트 활성 구현은 `/mnt/d/Projects/book-api-work/today-us/today-us-front` 쪽이다.
- 예전 `frontend/book-api-work-frontend`는 레거시 참고용이다.
