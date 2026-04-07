# 오늘 우리 백엔드 API 계약 초안 v1

작성일: 2026-04-07
상태: Draft
대상: `backend/book-api-work-backend`
연결 문서:
- `docs/specs/2026-04-05-today-our-mvp-v2.md`
- `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
- `docs/specs/2026-04-07-today-us-front-progress-v1.md`
- `docs/specs/2026-04-07-cross-env-handoff-v1.md`
- `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`

---

## 1. 목적

이 문서는 현재 `today-us-front`에서 잠근 상태 계약을 백엔드 API 기준으로 옮긴 초안이다.

핵심은 프론트 mock 상태와 실제 API 응답이 다른 말을 하지 않게 만드는 것이다.
특히 아래 4가지를 backend 계약에 명시적으로 반영한다.

1. 피드는 언제나 `현재 관계`만 보여준다.
2. 연결을 끊어도 이전 기록은 삭제하지 않는다.
3. 다시 연결하면 현재 관계의 기록 수와 책 진행도는 `0일부터 다시 시작`한다.
4. 보관함은 `현재 연결 기록`과 `이전 연결 기록`을 구분해서 보여준다.

---

## 2. 핵심 도메인 원칙

### 2.1 관계 인스턴스 원칙

- backend에서 `couples`는 단순히 "두 사람 조합"이 아니라 `한 번의 관계 인스턴스`로 본다.
- 즉, 연결을 끊은 뒤 다시 연결하면 기존 `couple_id`를 재사용하지 않고 새 `couple_id`를 만든다.
- 이 원칙을 쓰면 `현재 관계`와 `이전 관계`를 자연스럽게 분리할 수 있다.

### 2.2 현재 관계 원칙

- 한 사용자에게 동시에 활성화된 `ACTIVE` 관계는 최대 1개다.
- `GET /api/v1/me/home`과 책 진행도는 항상 `ACTIVE` 관계만 기준으로 계산한다.
- `ACTIVE` 관계가 없으면 피드/책 진행도는 빈 상태로 내려준다.

### 2.3 이전 기록 보존 원칙

- 관계를 해제해도 이전 `day_cards`, `card_entries`, `book_snapshots`, `orders`는 삭제하지 않는다.
- 관계 해제는 `soft delete`가 아니라 `관계 종료`로 해석한다.
- 종료된 관계는 `UNLINKED` 상태로 남고, 보관함 조회에서만 다시 노출된다.

### 2.4 재연결 원칙

- 사용자가 다시 연결되면 새 `ACTIVE couple`이 생성된다.
- 새 관계는 첫날부터 다시 시작한다.
- 따라서 기록 수, 함께한 일수, 책 진행도, snapshot eligibility는 새 `couple_id` 기준으로 다시 계산한다.

---

## 3. 관계 전환 상태 머신

```text
NO_ACTIVE_RELATIONSHIP
  -> invite_created
  -> invite_accepted
  -> ACTIVE

ACTIVE
  -> unlink_requested
  -> UNLINKED

UNLINKED
  -> new_invite_created
  -> new_invite_accepted
  -> ACTIVE (new couple_id)
```

규칙:

- `UNLINKED -> ACTIVE`는 기존 row 재오픈이 아니라 새 row 생성이다.
- 과거 관계 row는 archive 조회용 히스토리다.
- front의 `activeCycleId` 개념은 backend에선 사실상 `ACTIVE couple_id`로 대응된다.

---

## 4. 데이터 모델 반영 사항

기존 blueprint의 테이블 초안은 유지하되, 아래 해석을 추가한다.

### 4.1 `couples`

필드 해석:

- `id`: 관계 인스턴스 ID
- `status`: `ACTIVE | UNLINKED`
- `unlinked_at`: 관계 종료 시각

추가 규칙:

- 한 user는 `ACTIVE couple`에 하나만 속할 수 있다.
- 같은 두 사용자가 다시 연결돼도 새 `couples.id`를 만든다.

### 4.2 `couple_members`

필드 해석:

- `joined_at`: 해당 관계 인스턴스에 합류한 시점

추가 규칙:

- 과거 `UNLINKED` 관계의 member row는 유지한다.
- archive 조회는 이 membership history를 기준으로 끌어온다.

### 4.3 `day_cards`

현재 정의 유지:

- `day_cards.couple_id`는 항상 특정 관계 인스턴스를 가리킨다.

효과:

- 현재 관계 피드와 이전 관계 보관함을 별도 테이블 없이 분리할 수 있다.
- 재연결 시 책 진행도와 최근 기록이 자동으로 새 관계 기준으로 초기화된다.

### 4.4 `book_snapshots`

추가 규칙:

- snapshot eligibility는 항상 `ACTIVE couple_id`의 최근 30일 기록만 기준으로 계산한다.
- `UNLINKED` 관계의 snapshot은 보존하지만, 현재 주문 후보 계산에는 섞지 않는다.

---

## 5. 프론트 정렬 기준 API surface

### 5.1 홈

`GET /api/v1/me/home`

목적:

- 피드 첫 화면에 필요한 현재 관계 상태만 반환

반환 원칙:

- archived relation record를 포함하지 않는다.
- 최근 기록은 현재 `ACTIVE couple`의 최근 기록만 포함한다.
- book progress도 현재 `ACTIVE couple` 기준이다.

예시 shape:

```json
{
  "relationship": {
    "state": "connected",
    "coupleId": "cpl_active_20260407",
    "myName": "지우",
    "partnerName": "서윤",
    "startDate": "2026-04-07",
    "inviteCode": "TODAY3030"
  },
  "todayCard": {
    "localDate": "2026-04-07",
    "state": "mine-only",
    "me": {
      "author": "지우",
      "photoUrl": null,
      "emotionCode": "calm",
      "memo": "새 관계에서 처음 남긴 기록이에요."
    },
    "partner": null
  },
  "recentMoments": [
    {
      "localDate": "2026-04-07",
      "state": "PARTIAL",
      "me": { "author": "지우" },
      "partner": null
    }
  ],
  "bookProgress": {
    "lookbackDays": 30,
    "requiredDays": 20,
    "recordedDays": 1,
    "remainingDays": 19,
    "state": "growing"
  }
}
```

### 5.2 보관함

`GET /api/v1/archive`

목적:

- 현재 관계 기록과 이전 관계 기록을 나눠서 반환

반환 원칙:

- 기본 응답은 `sections` 배열을 사용한다.
- 각 section은 `current` 또는 `archived` 타입을 가진다.
- archived section에는 현재 파트너 이름을 섞지 않는다.

예시 shape:

```json
{
  "sections": [
    {
      "type": "current",
      "title": "현재 연결 기록",
      "description": "지금 함께 쌓고 있는 기록이에요.",
      "count": 4,
      "records": []
    },
    {
      "type": "archived",
      "title": "이전 연결 기록",
      "description": "연결이 끝난 뒤에도 이전 기록은 보관함에 남아 있어요.",
      "recordLabel": "이전 파트너와의 기록",
      "count": 18,
      "records": []
    }
  ]
}
```

### 5.3 연결 생성

`POST /api/v1/couples/invites`

목적:

- 새 `ACTIVE couple`을 만들기 위한 초대 생성

규칙:

- 활성 관계가 있으면 실패시킨다.
- 활성 관계가 없으면 새 `couple` row와 `invite`를 만든다.

### 5.4 연결 수락

`POST /api/v1/couples/invites/accept`

목적:

- 초대 수락으로 새 관계 활성화

규칙:

- 초대 수락 시 해당 `couple`이 `ACTIVE`가 된다.
- 사용자가 과거에 다른 관계를 가졌더라도 새 관계 기록 수는 0일부터 시작한다.

### 5.5 관계 해제

`POST /api/v1/couples/current/unlink`

목적:

- 현재 관계를 종료하고 기록을 archive로 넘김

규칙:

- 현재 `ACTIVE` 관계를 `UNLINKED`로 전환한다.
- day card 삭제는 하지 않는다.
- 응답에는 현재 관계가 더 이상 없음을 명확히 내려준다.

예시 shape:

```json
{
  "relationship": {
    "state": "unconnected"
  },
  "archiveUpdated": true
}
```

### 5.6 하루 카드

`GET /api/v1/day-cards/today`
`PUT /api/v1/day-cards/{localDate}/entry`
`GET /api/v1/day-cards/{localDate}`
`GET /api/v1/day-cards`

규칙:

- 모든 day card API는 현재 `ACTIVE couple` 기준이다.
- `GET /api/v1/day-cards` 기본 조회는 archived 관계를 포함하지 않는다.
- archived 조회가 필요하면 보관함 endpoint로 간다.

### 5.7 북

`GET /api/v1/book-snapshots/current`
`POST /api/v1/book-snapshots`
`POST /api/v1/book-snapshots/{snapshotId}/build`
`GET /api/v1/book-snapshots/{snapshotId}`

규칙:

- current snapshot은 현재 `ACTIVE couple` 기준으로만 본다.
- archived couple의 과거 snapshot은 주문 상세 또는 archive history에서만 확인한다.

---

## 6. 프론트 상태와의 매핑

| 프론트 개념 | backend source of truth |
|---|---|
| `relationshipState` | active couple 존재 여부 + invite 상태 |
| `moments` | active `couple_id`의 `day_cards` |
| `archiveMoments` | active + archived `couple_id`들의 recorded `day_cards` |
| `activeCycleId` | active `couple_id` |
| `bookProgress` | active `couple_id`의 최근 30일 eligibility 계산 |
| `current archive section` | active `couple_id` 기반 archive section |
| `archived archive section` | user membership history 중 `UNLINKED` 관계들 |

---

## 7. 응답 모델에서 꼭 지켜야 할 것

1. 홈 응답에 archived 기록을 섞지 않는다.
2. archive 응답은 current / archived를 구조적으로 분리한다.
3. unlink는 삭제가 아니라 상태 전환이어야 한다.
4. reconnect는 새 `couple_id`를 사용해야 한다.
5. book progress는 active 관계 기준으로만 계산해야 한다.
6. 파트너 이름이 archived card의 빈 칸 안내문에 현재 파트너 이름으로 잘못 노출되지 않게 해야 한다.

---

## 8. 백엔드 구현 전 체크리스트

1. `couples`를 재사용 가능한 영구 pair identity로 볼지, 관계 인스턴스로 볼지 팀 합의
2. 한 user당 `ACTIVE couple` 하나 제한을 DB와 서비스 양쪽에서 강제할지 결정
3. archive 응답을 BFF 스타일 view model로 줄지, resource 조합형으로 줄지 결정
4. `GET /api/v1/me/home`이 feed 전용 응답인지 범용 home summary인지 확정
5. unlink/reconnect 시 기존 invite code 재사용 금지 여부 결정

---

## 9. 현재 결론

프론트 기준으로는 `현재 관계`와 `이전 관계`를 섞는 순간 UX가 바로 흔들린다.

그래서 backend도 아래 문장 하나로 정리돼야 한다.

`현재 화면은 active couple이 책임지고, 이전 기록은 archived couple이 책임진다.`

이 기준이 잠기면 프론트 mock에서 맞춘 감정선이 실제 API 연동 이후에도 유지된다.
