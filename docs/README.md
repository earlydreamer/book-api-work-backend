# 백엔드 docs 안내

이 디렉터리는 `book-api-work-backend` 저장소 안에서 바로 구현을 이어갈 수 있게 백엔드 계약과 프론트 맥락을 함께 묶어둔 곳이다.

## 1. 문서 분류

### 공통/연동 문서

- `docs/specs/2026-04-05-today-our-mvp-v2.md`
- `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`
- `docs/specs/2026-04-07-cross-env-handoff-v1.md`
- `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`

이 문서들은 frontend와 backend 두 저장소에 함께 둔다.
계약이 바뀌면 양쪽 사본을 같이 갱신한다.

### 프론트 참고 문서

- `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
- `docs/specs/2026-04-07-today-us-front-progress-v1.md`
- `docs/plans/2026-04-07-today-us-front-alignment-roadmap.md`
- `docs/references/2026-04-07-today-us-design-system.md`

이 문서들은 backend가 프론트 상태 계약과 화면 모델을 이해하기 위해 로컬 참고 사본으로 보관한다.
프론트 기준 판단은 frontend repo 사본을 우선한다.

## 2. 권장 읽기 순서

1. `../AGENTS.md`
2. `../README.md`
3. `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
4. `docs/specs/2026-04-07-cross-env-handoff-v1.md`
5. `docs/specs/2026-04-07-today-us-front-progress-v1.md`
6. `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
7. `docs/references/2026-04-07-today-us-design-system.md`

## 3. 동기화 규칙

- API 계약 변경 시 frontend repo 사본도 같이 수정
- 관계 정책 변경 시 backend 계약 문서와 프론트 상태 문서를 함께 검토
- 프론트 문서를 backend에서 보강했으면 frontend 원본에도 반영할지 같이 판단
