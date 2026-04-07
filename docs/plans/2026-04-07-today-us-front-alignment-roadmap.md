# today-us 프론트 정렬 실행 로드맵

작성일: 2026-04-07
상태: Active
연결 문서:
- `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
- `docs/specs/2026-04-07-today-us-front-progress-v1.md`
- `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`

---

## 1. 목표

`today-us-front`를 단일 데모 목업에서 `본 구현 직전의 계약 검증 프론트`로 올린다.

핵심은 새 기능 추가가 아니다.
핵심은 `상태 모델`, `화면 우선순위`, `mock adapter`, `디자인 언어`를 먼저 정렬해서 이후 API 연동이 흔들리지 않게 만드는 것이다.

---

## 2. 작업 원칙

1. 화면을 먼저 고치지 않는다. 상태 모델을 먼저 고친다.
2. 페이지가 mock raw data를 직접 읽지 않게 한다.
3. 숫자 기준은 상수/selector 한 곳에 모은다.
4. 신규 유저 경험을 실제 헤비 유저 fixture로 덮어쓰지 않는다.
5. 책 만들기 경험은 유지하되, 일일 기록 루프를 이기지 않게 한다.

---

## 3. 우선순위

## P0. 제품 계약 정렬

상태: 완료

### 목표

- 상태 축 정의
- 표준 eligibility 규칙 통일
- 화면별 1순위 CTA 고정

### 결과물

- frontend 내부 도메인 상태 타입
- selector / constants
- 시나리오 목록

### 완료 기준

- 피드/책/연결 화면이 같은 제품 규칙을 쓴다.
- 현재 기준:
  - 상태 축, eligibility 기준, 피드 CTA 우선순위가 selector와 화면에서 통일됐다.
  - 연결 해제/재연결 정책도 계약으로 잠겼다.

---

## P1. mock adapter 도입

상태: 부분 완료

### 목표

- `globalMockMoments` 직참조 제거
- 페이지가 adapter만 읽도록 구조 변경

### 필요한 단위

- `mock/scenarios/*`
- `adapters/authAdapter`
- `adapters/homeAdapter`
- `adapters/bookAdapter`
- `adapters/recordAdapter`
- `adapters/settingsAdapter`

### 완료 기준

- 페이지 컴포넌트에서 raw fixture import가 사라진다.
- 현재 기준:
  - 페이지는 대부분 컨텍스트와 selector를 통해 상태를 읽는다.
  - 다만 문서상 분리한 `adapters/*` 구조는 아직 만들지 않았다.

---

## P2. 온보딩과 피드 재배치

상태: 대부분 완료

### 목표

- 신규 유저 감정선 회복
- 연결 전 / 연결 후 / 첫 기록 전 상태 분리

### 손볼 화면

- Landing
- Signup
- Login
- Connect
- Feed

### 핵심 수정

- 랜딩의 기능 나열형 teaser 축소
- 가입/인증의 인라인 상태화
- 연결 직후 피드에서 `첫 기록`을 1순위로 끌어올리기
- 연결 전에는 책 성취 경험을 약하게, 연결/기록 경험을 강하게

### 완료 기준

- 막 가입한 유저가 `이미 60개 기록을 가진 사람`처럼 보이지 않는다.
- 현재 기준:
  - Signup, Connect, Feed의 상태 분기가 정리됐다.
  - 랜딩 teaser 축소는 아직 추가 정리가 남아 있다.

---

## P3. 책 경험 2단 분리

상태: 완료

### 목표

- `성장 중인 책`과 `주문 가능한 책`을 분리

### 손볼 화면

- Feed
- MakeBook

### 핵심 수정

- 성장 중: 진행률, 남은 조건, 디지털 미리보기
- 주문 가능: 스냅샷 생성, 조판, 결제
- 하드락 empty state 하나로 끝내지 않기

### 완료 기준

- 주문 가능 전에도 `/make-book`이 가치 있는 화면이다.
- 주문 가능 이후에는 바로 실행 가능한 흐름이 있다.
- 현재 기준:
  - MakeBook은 성장 구간과 주문 실행 구간을 분리해서 보여준다.
  - Feed도 같은 eligibility 기준으로 연결됐다.

---

## P4. 디자인 언어 정리

상태: 부분 완료

### 목표

- 제품 톤을 문서와 구현이 같은 방향으로 맞춘다.

### 수정 대상

- 영어 라벨 제거
- placeholder 링크 제거
- alert / confirm 제거
- 아이콘 서클형 teaser 축소
- 메타/보조 CTA 강도 조절

### 완료 기준

- 핵심 플로우 안에 브라우저 기본 UI가 없다.
- 제품 내 언어가 거의 전부 한국어다.
- 현재 기준:
  - UX 라이팅 기준과 앱 카피 톤은 정리됐다.
  - placeholder 링크와 랜딩 teaser 일부는 추가 정리가 필요하다.

---

## P5. 테스트 바닥 깔기

상태: 부분 완료

### 목표

- 상태 정렬 작업이 다시 무너지지 않게 한다.

### 최소 테스트 대상

- 인증 복원
- 연결 상태별 피드 분기
- 오늘 카드 상태별 hero 분기
- 책 자격 계산
- 주문 가능 전/후 MakeBook 분기

### 완료 기준

- `tsc`만이 아니라 상태 분기 테스트가 존재한다.
- 현재 기준:
  - 책 자격 계산, 부분 카드 상태, 연결 해제/재연결, 보관함 구분, 피드 current-only 노출 테스트가 있다.
  - Auth hydrate, MakeBook 분기 테스트는 더 필요하다.

---

## P6. backend 계약 동기화

상태: 시작

### 목표

- 프론트에서 잠근 상태 계약을 backend API 계약으로 옮긴다.

### 결과물

- 관계 전환 정책이 반영된 API 계약 문서
- current/archive 분리 응답 모델
- unlink/reconnect 정책 정의

### 완료 기준

- `현재 관계`와 `이전 관계` 구분이 backend 문서에도 명시된다.
- 홈/보관함/책 API가 같은 상태 모델을 사용한다.

---

## 4. 권장 실행 순서

```text
1. 상태 타입/selector/constants 고정
2. scenario fixtures 분리
3. adapter 도입
4. Feed / Connect / Record 재배치
5. MakeBook 2단 구조 정리
6. Landing / Auth / Me 카피와 상태 UX 정리
7. 테스트 추가
8. 그 다음 API 계약 연결
```

이 순서를 바꾸면 안 되는 이유는 간단하다.

- adapter 없이 화면부터 고치면 다시 raw fixture 의존으로 돌아간다.
- eligibility 규칙을 늦게 고치면 Feed와 MakeBook이 또 서로 다른 말을 한다.
- 테스트를 맨 마지막으로 미루면 상태 정렬 작업이 다시 깨진다.

---

## 5. 리스크

### 리스크 1. mock 단계인데 너무 많이 고치는 것 아니냐

아니다.
지금 하는 건 디테일 polish가 아니라 `상태 계약 정리`라서, 오히려 지금 해야 싸다.

### 리스크 2. 디자인이 심심해질 수 있다

가능하다.
그래도 지금은 `기록 루프가 먼저`다.
책 만들기 과장으로 얻는 화려함보다, 사용자가 지금 무엇을 해야 하는지 분명한 게 더 중요하다.

### 리스크 3. backend 계약 전에 frontend 모델을 잠그면 나중에 바뀌지 않나

일부는 바뀔 수 있다.
그래도 `상태 축`, `CTA 우선순위`, `eligibility 단일 기준`은 지금 잠가야 한다.
이건 backend보다 위의 제품 계약이기 때문이다.

---

## 6. 이번 라운드 완료 기준

아래를 만족하면 `today-us-front`는 본 구현 직전 단계로 본다.

1. 신규 유저, 연결 대기 유저, 활성 커플 유저가 서로 다른 경험을 가진다.
2. Feed와 MakeBook이 같은 자격 기준을 쓴다.
3. 새로고침 후에도 mock 세션과 시나리오가 유지된다.
4. 페이지가 raw mock 배열을 직접 import하지 않는다.
5. 책 경험이 `성장`과 `주문`으로 분리된다.
6. 영어 임시 라벨과 브라우저 기본 alert/confirm이 제거된다.
7. 최소 상태 분기 테스트가 있다.

현재 판정:

- `1 ~ 6`: 충족
- `7`: 부분 충족
- 추가로 `backend 계약 동기화 문서`까지 초안 작성 단계로 들어갔다.

---

## 7. 다음 액션

바로 구현에 들어갈 때는 아래 순서가 가장 안전하다.

1. `backend API contract`를 기준으로 프론트 mock adapter shape 고정
2. `scenario fixture + adapters/*` 구조를 문서대로 실제 분리
3. `Auth hydrate`, `MakeBook` 테스트 보강
4. 랜딩 teaser / placeholder 링크 정리
5. backend repo에서 `users / couples / day_cards / snapshots` 계약 타입 고정

즉, 이번 프론트 정렬의 본체는 `디자인 수정`이 아니라 `상태 수정`이다.
