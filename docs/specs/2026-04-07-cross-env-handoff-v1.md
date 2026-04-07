# 오늘 우리 교차 환경 handoff v1

작성일: 2026-04-07
상태: Active
목적: 다른 환경, 다른 세션, 다른 에이전트가 바로 이어서 작업할 수 있게 현재 맥락을 한 문서에 묶는다.

연결 문서:
- `AGENTS.md`
- `docs/specs/2026-04-05-today-our-mvp-v2.md`
- `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`
- `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
- `docs/specs/2026-04-07-today-us-front-progress-v1.md`
- `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
- `docs/references/2026-04-07-today-us-design-system.md`

---

## 1. 이 문서가 필요한 이유

이 프로젝트는 현재 `활성 프론트 repo + 백엔드 repo + 레거시 프론트`가 함께 있는 과도기 상태다.

그래서 다른 환경에서 이어받을 때 가장 쉽게 생기는 오해가 아래 4개다.

1. 어느 프론트 디렉터리가 현재 기준인지
2. backend가 어느 정도 구현됐는지
3. 어떤 문서가 최신 source of truth인지
4. 연결 해제/재연결 정책이 어디까지 잠겼는지

이 문서는 그 오해를 없애기 위한 cross-env 시작점이다.

---

## 2. 지금 workspace를 한 줄로 설명하면

`today-us/today-us-front`는 현재 상태 계약이 정리된 활성 프론트 구현체이고,
`backend/book-api-work-backend`는 contract endpoint와 JPA core slice가 들어간 Spring Boot repo이며,
공통 문서는 이 repo의 `docs/`에 로컬 사본으로 들어 있고, frontend repo에도 대응 사본이 있다.

---

## 3. 디렉터리와 역할

### 3.1 루트

`/mnt/d/Projects/book-api-work`

- workspace 진입점
- nested repo 묶음
- 프론트/백엔드/레거시 디렉터리를 함께 가리키는 workspace

주의:

- 루트 자체는 git repo가 아닐 수 있다.
- git 작업은 nested repo 기준으로 확인해야 한다.

### 3.2 활성 프론트

`/mnt/d/Projects/book-api-work/today-us/today-us-front`

- 현재 프론트 작업 기준
- React + TypeScript + Vite
- mock session / selector / 상태 계약이 이미 일부 구현됨

### 3.3 레거시 프론트

`/mnt/d/Projects/book-api-work/frontend/book-api-work-frontend`

- 참고용
- 새 기능 개발 기준 아님

### 3.4 백엔드

`/mnt/d/Projects/book-api-work/backend/book-api-work-backend`

- nested git repo
- 2026-04-07 기준 Spring Boot contract scaffold + JPA core slice 존재
- endpoint, DTO, validation, ProblemDetail, security toggle, OpenAPI, JPA entity/repository, Flyway migration까지 들어간 상태
- local 기본 실행은 H2 + Flyway 기준
- Supabase JWT 검증과 운영 Postgres 연결은 다음 슬라이스

---

## 4. source of truth 읽는 순서

### 4.1 공통 기준

1. `AGENTS.md`
2. `docs/specs/2026-04-05-today-our-mvp-v2.md`
3. `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`

### 4.2 프론트 이어받을 때

1. `docs/references/2026-04-07-today-us-design-system.md`
2. `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
3. `docs/specs/2026-04-07-today-us-front-progress-v1.md`
4. frontend repo의 `README.md`

### 4.3 백엔드 이어받을 때

1. `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
2. `README.md`
3. `docs/specs/2026-04-07-today-us-front-progress-v1.md`

읽는 이유:

- 프론트 정책이 backend 계약보다 먼저 잠겨 있는 부분이 있다.
- backend는 프론트가 이미 잠근 관계 정책을 깨지 않는 방향으로 가야 한다.

---

## 5. 현재 프론트 상태 요약

### 5.1 이미 구현된 것

- 세션 복원
- 책 자격 selector 통일
- 피드 상태 재정렬
- 기록 저장 flow
- 책 성장/주문 구간 분리
- 부분 카드 UX 정리
- UX 라이팅 톤 정리
- 연결 해제/재연결 정책 반영
- 보관함 current/archive 구분
- 피드 current-only 노출 보장

### 5.2 현재 핵심 정책

- 피드는 항상 현재 관계 기록만 보여준다.
- 보관함은 현재 관계와 이전 관계를 분리한다.
- 연결을 끊어도 기록은 삭제하지 않는다.
- 다시 연결하면 기록 수와 책 진행도는 0일부터 다시 시작한다.
- 책 자격 기준은 최근 30일 중 20일 기록이다.

### 5.3 프론트 검증 기준

실행 디렉터리:

`/mnt/d/Projects/book-api-work/today-us/today-us-front`

명령:

```bash
npm install
npm run dev
npm test -- --run
npm run lint
npm run build
```

2026-04-07 마지막 확인:

- `npm test -- --run` 통과, `17 passed`
- `npm run lint` 통과
- `npm run build` 통과

### 5.4 중요한 파일

- `src/context/AuthContext.tsx`
- `src/context/sessionState.ts`
- `src/mock/selectors.ts`
- `src/pages/Feed.tsx`
- `src/pages/Archive.tsx`
- `src/pages/MakeBook.tsx`
- `src/components/MomentCard.tsx`

---

## 6. 현재 백엔드 상태 요약

### 6.1 이미 정해진 것

- Spring Boot 기반으로 감
- JWT/stateless auth
- Supabase Auth + Postgres
- day card / snapshot / order 중심 구조
- Sweetbook/Toss는 서버에서만 호출

### 6.2 아직 구현되지 않은 것

- 실제 Supabase JWT 검증 연결
- 운영 Postgres datasource와 환경별 설정 분리
- snapshot/order write path
- R2 presigned upload flow
- 외부 제작/결제 연동

### 6.3 backend가 반드시 따라야 할 현재 정책

- `GET /api/v1/me/home`은 archived 기록을 섞지 않는다.
- `GET /api/v1/archive`는 current / archived를 분리해 내려준다.
- unlink는 삭제가 아니라 관계 종료다.
- reconnect는 현재 초안 기준으로 새 `couple_id`를 만든다.
- book progress는 현재 활성 관계 기준으로만 계산한다.

---

## 7. 현재 가장 중요한 열린 결정

### 7.1 reconnect 시 새 `couple_id` 생성 가정

현재 문서는 이 가정을 중심으로 정리돼 있다.

이게 좋은 이유:

- 현재 관계와 이전 관계를 DB 레벨에서 가장 명확하게 분리할 수 있다.
- 피드, 책 진행도, 최근 기록 계산이 자연스럽다.

지금 JPA 구현도 이 가정을 기준으로 들어가 있다.
만약 뒤집으면 `couples` migration, archive 조회, reconnect service 로직을 같이 바꿔야 한다.

### 7.2 home/archive 응답 모델 스타일

지금 초안은 프론트 친화적인 BFF 응답에 가깝다.

결정 필요:

- 프론트에 바로 쓰기 쉬운 view model로 갈지
- resource 단위 endpoint 조합형으로 갈지

### 7.3 AI Studio 잔재 정리

프론트에는 아직 AI Studio scaffold 흔적이 일부 남아 있다.

- `.env.example`
- `vite.config.ts`
- 일부 의존성

이건 현재 상태 계약과 직접 충돌하진 않지만, 다른 환경에서 바로 이어받을 때 혼란을 줄 수 있으니 정리 우선순위가 꽤 높다.

---

## 8. 예전 문서와 현재 구현 사이의 차이

### 8.1 실행 청사진

`docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`는 여전히 유효한 큰 그림 문서다.
다만 작성 시점에는 활성 프론트 구현이 없었기 때문에, 현재 프론트 위치와 상태는 이 문서 하나만 보면 부족할 수 있다.

그래서 반드시 아래 문서를 같이 봐야 한다.

- `docs/specs/2026-04-07-today-us-front-progress-v1.md`
- `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
- 이 handoff 문서

### 8.2 레거시 frontend 폴더

예전 `frontend/book-api-work-frontend`는 현재 기준이 아니다.
UI/상태 규칙을 참고로만 봐야 한다.

---

## 9. 새 환경에서 바로 시작할 때 체크리스트

### 프론트부터 이어받는 경우

1. frontend repo의 `README.md` 읽기
2. `docs/specs/2026-04-07-today-us-front-progress-v1.md` 읽기
3. `npm install`
4. `npm test -- --run`
5. `npm run lint`
6. `npm run build`
7. 그다음 수정 시작

### 백엔드부터 이어받는 경우

1. `README.md` 읽기
2. `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md` 읽기
3. `docs/plans/2026-04-07-today-us-backend-contract-scaffold-plan.md` 읽기
4. `./gradlew test`
5. `./gradlew build`
6. Supabase JWT / 운영 Postgres / snapshot-order 확장 순서로 이어가기

---

## 10. 추천 다음 순서

1. contract endpoint를 프론트 adapter와 실제 fetch layer에 연결
2. Supabase JWT 검증과 local auth fallback 분리
3. `book_snapshots`, `orders` persistence 모델 확장
4. Auth hydrate / MakeBook 테스트 보강
5. AI Studio 잔재 제거

---

## 11. 마지막 메모

이 프로젝트는 지금 `예쁜 화면`보다 `상태 계약 잠금`이 더 중요하다.

그래서 이어받는 사람도 아래 문장 하나만 기억하면 된다.

`현재 관계의 루프와 이전 기록의 보관을 분리해서 생각해야 한다.`
