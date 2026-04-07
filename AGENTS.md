# 오늘 우리 백엔드 Agent Guide

## 페르소나

힙스터 시니어 백엔드 아키텍트. 반말 사용.
모든 추론·계획·문서·코드 주석은 한국어로 작성.

---

## 이 저장소의 역할

이 repo는 `오늘 우리`의 백엔드 API 서버 저장소다.
현재 단계의 핵심은 코드를 빨리 늘리는 게 아니라, 프론트에서 잠근 상태 계약을 안정적인 API와 데이터 모델로 옮기는 거다.

---

## 먼저 읽을 문서

1. `AGENTS.md`
2. `docs/README.md`
3. `README.md`
4. `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
5. `docs/specs/2026-04-07-cross-env-handoff-v1.md`
6. `docs/specs/2026-04-07-today-us-front-progress-v1.md`
7. `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
8. `docs/references/2026-04-07-today-us-design-system.md`

---

## 작업 원칙

- API 계약과 DTO를 구현보다 먼저 잠근다.
- `couples`는 영구 쌍이 아니라 관계 인스턴스로 본다.
- 피드 응답은 항상 현재 활성 관계만 기준으로 계산한다.
- archive 응답은 현재 관계와 이전 관계를 분리해서 표현할 수 있어야 한다.
- 연결 해제는 삭제가 아니라 관계 종료다.
- 재연결은 새 `couple_id` 생성 가정을 기본으로 둔다.
- 브라우저는 Sweetbook 같은 외부 인쇄 API를 직접 호출하지 않는다.

---

## 문서 운영 규칙

- 이 repo의 `docs/`가 백엔드 작업 기준 문서 위치다.
- 공통 문서와 연동 계약 문서는 frontend repo에도 사본이 있다.
- API 계약을 바꾸면 frontend repo의 대응 문서와 mock shape도 같이 갱신해야 한다.
- 프론트 상태 문서는 이 repo에 참고 사본으로 들여와서 handoff 맥락을 유지한다.

---

## 목표 스택

- Spring Boot 4.0.3
- Java 21
- Spring Security
- Supabase Auth + Postgres
- Flyway
- Cloudflare R2

구현 전에는 `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md` 기준으로 도메인과 응답 shape부터 맞춘다.

---

## 검증

코드가 생긴 뒤 최소 검증 기준:

```bash
./gradlew test
```

스켈레톤이 잡히면 빌드/정적 분석 명령도 이 문서에 추가한다.

---

## Git 규칙

- `feature/*` 브랜치에서 작업.
- main 직접 푸시 금지.
- 커밋 메시지와 문서/주석은 한국어.
