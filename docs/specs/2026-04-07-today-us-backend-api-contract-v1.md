# 오늘 우리 백엔드 API 계약 정리본

작성일: 2026-04-07
상태: Reviewed Draft
대상: `backend/book-api-work-backend`
기준 프론트: `today-us/today-us-front`
연결 문서:
- `docs/specs/2026-04-05-today-our-mvp-v2.md`
- `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
- `docs/specs/2026-04-07-today-us-front-progress-v1.md`
- `docs/specs/2026-04-07-cross-env-handoff-v1.md`
- `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`

---

## 1. 이 문서가 하는 일

이 문서는 `today-us-front`의 목업 구현을 실제 백엔드 계약으로 내리는 기준선이다.

기존 초안이 방향을 잠그는 문서였다면, 이번 정리본은 아래 3가지를 더 분명하게 한다.

1. 프론트가 실제로 읽는 화면 상태가 어떤 endpoint와 DTO로 내려와야 하는지
2. 지금 기준 구현 범위가 어디까지 들어왔는지
3. 다음 슬라이스로 미루는 항목이 무엇인지

핵심 목표는 간단하다.

`프론트 mock selector가 읽는 상태를 백엔드 응답도 같은 말로 설명하게 만든다.`

---

## 2. 프론트에서 역산한 핵심 요구

### 2.1 현재 관계와 이전 관계를 절대 섞지 않는다

- 피드는 항상 `현재 ACTIVE 관계`만 기준으로 계산한다.
- 보관함은 `현재 연결 기록`과 `이전 연결 기록`을 별도 섹션으로 내려준다.
- 연결 해제는 삭제가 아니라 관계 종료다.
- 재연결은 기존 관계 재오픈이 아니라 새 관계 인스턴스 생성이다.

### 2.2 프론트는 이미 BFF 응답을 기대하고 있다

`today-us-front`는 raw resource를 여기저기 조합하는 구조가 아니라, 화면이 바로 읽을 수 있는 파생 상태를 많이 쓴다.

그래서 이번 계약은 아래 원칙으로 간다.

- `GET /api/v1/me/home`는 피드 화면 전용 BFF 응답이다.
- `GET /api/v1/archive`는 보관함 전용 sectioned view model이다.
- `GET /api/v1/book-snapshots/current`는 책 탭 전용 요약 응답이다.

이 선택이 맞는 이유:

- 프론트가 이미 `relationshipState`, `todayCardState`, `bookState`를 화면 중심으로 읽고 있다.
- 아직 백엔드 구현 초반이라 resource 조합형으로 쪼개면 프론트 adapter 복잡도만 먼저 커진다.
- 지금 단계의 목표는 재사용성보다 `상태 계약 고정`이다.

### 2.3 인증은 Supabase가 맡고, 백엔드는 JWT 이후의 상태를 맡는다

- 로그인, 회원가입, 이메일 인증은 프론트의 `Supabase JS`가 직접 담당한다.
- 백엔드는 별도 회원가입/로그인 endpoint를 만들지 않는다.
- 백엔드는 `Authorization: Bearer <access-token>` 기준으로 현재 사용자를 식별한다.
- 지금 단계에서는 JWT 검증 토글을 두고, auth-disabled local 모드에서는 `X-Today-Us-Auth-User-Id` 헤더로 테스트 사용자를 고를 수 있게 한다.

---

## 3. 이번 라운드에서 잠그는 결정

### 3.1 관계 인스턴스 원칙

- `couples`는 영구 pair identity가 아니라 `한 번의 관계 인스턴스`다.
- 같은 두 사용자가 다시 연결돼도 새 `couple_id`를 만든다.
- `UNLINKED -> ACTIVE`는 row 재오픈이 아니라 새 row 생성이다.

### 3.2 홈/보관함/책은 BFF 응답으로 고정

- 홈은 `relationship + todayCard + recentMoments + bookProgress`
- 보관함은 `current` / `archived` section 배열
- 책은 `bookProgress + candidateMoments + order readiness`

### 3.3 현재 구현 범위

지금 기준으로 구현된 건 아래까지다.

- Spring Boot 프로젝트 뼈대
- controller / service / dto / entity / repository / config 패키지 구조
- 프론트 연동용 contract endpoint
- validation, ProblemDetail 기반 에러 shape, OpenAPI 노출
- JPA entity / repository / Flyway core migration
- local H2 + Flyway 기준 실행 가능한 개발 모드

아직 남아 있는 건 아래다.

- 실제 Supabase JWT 검증 연결
- 운영 Postgres datasource와 환경별 설정 분리
- R2 presigned upload flow
- 실제 Sweetbook / Toss / job_queue 구현

---

## 4. 공통 규칙

### 4.1 시간대와 날짜

- 모든 사용자 노출 날짜는 `Asia/Seoul` 기준 `localDate`로 계산한다.
- `localDate`는 `yyyy-MM-dd` 문자열로 주고받는다.
- 하루 카드 마감 기준은 명세대로 `새벽 4시`를 목표로 두되, 현재 구현은 저장 시점 기준 `close_at_utc`를 계산해 day-card row에 보관한다.

### 4.2 공통 enum

| 항목 | 값 |
|---|---|
| `relationship.state` | `unconnected`, `invite-pending`, `connected` |
| `todayCard.state` | `empty`, `mine-only`, `partner-only`, `complete`, `closed` |
| `bookProgress.state` | `growing`, `eligible`, `snapshot-building`, `ready-to-order`, `ordered` |
| `archive.sections[].type` | `current`, `archived` |
| `record.state` | `partial`, `complete`, `closed` |

### 4.3 공통 에러 모델

Spring Boot 기본 `ProblemDetail`을 베이스로 쓰고, 아래 필드를 추가한다.

```json
{
  "type": "https://api.todayus.dev/problems/invite-not-found",
  "title": "초대 코드를 찾을 수 없어요.",
  "status": 404,
  "detail": "만료됐거나 존재하지 않는 초대 코드예요.",
  "instance": "/api/v1/couples/invites/TODAY9999",
  "code": "invite_not_found",
  "traceId": "9d5d4d77a4b6b7c1",
  "fieldErrors": []
}
```

규칙:

- validation 오류는 `code = validation_failed`
- 활성 관계 충돌은 `code = active_relationship_conflict`
- 초대 코드 없음은 `code = invite_not_found`
- 현재 관계 없음은 `code = relationship_not_found`

### 4.4 응답 설계 원칙

- 프론트가 바로 렌더링 가능한 표시용 문자열은 백엔드가 내려줄 수 있다.
- 하지만 문장 전체를 서버에서 조립하지는 않는다.
- title/description 같은 section copy는 이번 라운드에서 DTO에 포함한다.
- `recentMoments`, `archive.records`, `candidateMoments`는 모두 같은 record 요약 shape를 재사용한다.

---

## 5. API surface

| Method | Path | 목적 | 프론트 소비처 |
|---|---|---|---|
| `GET` | `/api/v1/me/home` | 피드 홈 요약 | `Feed.tsx` |
| `GET` | `/api/v1/archive` | 보관함 section 응답 | `Archive.tsx` |
| `POST` | `/api/v1/couples/invites` | 내 공간 생성 + 초대 코드 발급 | `Connect.tsx` create |
| `GET` | `/api/v1/couples/invites/{inviteCode}` | 초대 코드 미리보기 | `Connect.tsx` join-code |
| `POST` | `/api/v1/couples/invites/{inviteCode}/accept` | 초대 수락 | `Connect.tsx` join-name |
| `POST` | `/api/v1/couples/current/unlink` | 현재 관계 종료 | `Me.tsx` |
| `GET` | `/api/v1/day-cards/today` | 오늘 카드 조회 | `Feed.tsx`, `Record.tsx` |
| `PUT` | `/api/v1/day-cards/{localDate}/entry` | 오늘 내 기록 저장/수정 | `Record.tsx` |
| `GET` | `/api/v1/book-snapshots/current` | 책 탭 요약 | `MakeBook.tsx` |

---

## 6. DTO 상세

### 6.1 `GET /api/v1/me/home`

목적:

- 피드 첫 화면에 필요한 상태를 한 번에 준다.
- archived 관계 정보는 절대 섞지 않는다.

응답 예시:

```json
{
  "relationship": {
    "state": "connected",
    "coupleId": "cpl_active_20260407",
    "myName": "지우",
    "partnerName": "민준",
    "startDate": "2026-04-07",
    "inviteCode": "TODAY2026"
  },
  "todayCard": {
    "localDate": "2026-04-07",
    "dateLabel": "4월 7일 화",
    "state": "mine-only",
    "me": {
      "author": "지우",
      "emotionCode": "calm",
      "emotionEmoji": "🌿",
      "emotionLabel": "차분해",
      "memo": "새 관계에서 처음 남긴 기록이에요.",
      "photoUrl": null
    },
    "partner": null
  },
  "recentMoments": [
    {
      "id": "moment-cpl_active_20260407-2026-04-07",
      "localDate": "2026-04-07",
      "dateLabel": "4월 7일 화",
      "state": "partial",
      "me": {
        "author": "지우",
        "emotionCode": "calm",
        "emotionEmoji": "🌿",
        "emotionLabel": "차분해",
        "memo": "새 관계에서 처음 남긴 기록이에요.",
        "photoUrl": null
      },
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

관계 상태별 규칙:

- `unconnected`: `coupleId`, `partnerName`, `startDate`, `inviteCode`는 `null`
- `invite-pending`: `partnerName`은 `null`, `inviteCode`는 존재
- `connected`: 모든 관계 필드가 존재

### 6.2 `GET /api/v1/archive`

목적:

- 현재 관계 기록과 이전 관계 기록을 구조적으로 분리한다.

응답 예시:

```json
{
  "sections": [
    {
      "type": "current",
      "title": "현재 연결 기록",
      "description": "지금 함께 쌓고 있는 기록이에요.",
      "count": 4,
      "recordLabel": null,
      "records": [
        {
          "id": "moment-cpl_active_20260407-2026-04-07",
          "localDate": "2026-04-07",
          "dateLabel": "4월 7일 화",
          "state": "partial",
          "me": {
            "author": "지우",
            "emotionCode": "calm",
            "emotionEmoji": "🌿",
            "emotionLabel": "차분해",
            "memo": "새 관계에서 처음 남긴 기록이에요.",
            "photoUrl": null
          },
          "partner": null
        }
      ]
    },
    {
      "type": "archived",
      "title": "이전 연결 기록",
      "description": "연결이 끝난 뒤에도 이전 기록은 보관함에 남아 있어요.",
      "count": 18,
      "recordLabel": "이전 파트너와의 기록",
      "records": []
    }
  ]
}
```

규칙:

- archived section의 `partner` 빈 상태 문구에 현재 파트너 이름이 섞이면 안 된다.
- 응답에 month grouping을 미리 넣지 않는다. 월 grouping은 프론트 adapter에서 계산한다.
- 대신 `dateLabel`은 내려줘서 초기 구현 부담을 줄인다.

### 6.3 `POST /api/v1/couples/invites`

목적:

- 아직 파트너가 없는 사용자가 내 공간을 먼저 만들고 초대 코드를 발급받는다.

요청:

```json
{
  "startDate": "2026-04-07"
}
```

응답:

```json
{
  "relationship": {
    "state": "invite-pending",
    "coupleId": "cpl_pending_20260407",
    "myName": "지우",
    "partnerName": null,
    "startDate": "2026-04-07",
    "inviteCode": "TODAY2026"
  }
}
```

실패:

- 이미 `ACTIVE` 관계가 있으면 `409 active_relationship_conflict`

### 6.4 `GET /api/v1/couples/invites/{inviteCode}`

목적:

- 연결 전, 초대 코드가 유효한지 확인하고 상대 이름 미리보기를 준다.

응답:

```json
{
  "inviteCode": "TODAY2026",
  "inviterName": "민준",
  "startDate": "2026-04-07",
  "status": "invite-pending"
}
```

실패:

- 코드가 없거나 만료면 `404 invite_not_found`

### 6.5 `POST /api/v1/couples/invites/{inviteCode}/accept`

목적:

- 초대를 수락하고 새 관계를 활성화한다.

응답:

```json
{
  "relationship": {
    "state": "connected",
    "coupleId": "cpl_active_20260407",
    "myName": "지우",
    "partnerName": "민준",
    "startDate": "2026-04-07",
    "inviteCode": "TODAY2026"
  },
  "bookProgress": {
    "lookbackDays": 30,
    "requiredDays": 20,
    "recordedDays": 0,
    "remainingDays": 20,
    "state": "growing"
  }
}
```

규칙:

- 재연결이어도 새 `coupleId`를 쓴다.
- 새 관계의 recordedDays는 0부터 다시 시작한다.

### 6.6 `POST /api/v1/couples/current/unlink`

목적:

- 현재 관계를 종료하고 기록을 archive 쪽으로 넘긴다.

응답:

```json
{
  "relationship": {
    "state": "unconnected",
    "coupleId": null,
    "myName": "지우",
    "partnerName": null,
    "startDate": null,
    "inviteCode": null
  },
  "archiveUpdated": true,
  "bookProgress": {
    "lookbackDays": 30,
    "requiredDays": 20,
    "recordedDays": 0,
    "remainingDays": 20,
    "state": "growing"
  }
}
```

규칙:

- day card 삭제는 하지 않는다.
- 현재 관계가 없으면 `404 relationship_not_found`

### 6.7 `GET /api/v1/day-cards/today`

목적:

- 기록 화면과 피드가 오늘 카드만 따로 갱신할 수 있게 한다.

응답:

```json
{
  "localDate": "2026-04-07",
  "dateLabel": "4월 7일 화",
  "state": "mine-only",
  "me": {
    "author": "지우",
    "emotionCode": "calm",
    "emotionEmoji": "🌿",
    "emotionLabel": "차분해",
    "memo": "새 관계에서 처음 남긴 기록이에요.",
    "photoUrl": null
  },
  "partner": null
}
```

### 6.8 `PUT /api/v1/day-cards/{localDate}/entry`

목적:

- 현재 사용자 기준으로 하루 카드 엔트리를 저장하거나 수정한다.

요청:

```json
{
  "emotionCode": "calm",
  "memo": "사진 한 장 없이도 남길 수 있는 기록이에요.",
  "photoUrl": null
}
```

응답:

```json
{
  "todayCard": {
    "localDate": "2026-04-07",
    "dateLabel": "4월 7일 화",
    "state": "mine-only",
    "me": {
      "author": "지우",
      "emotionCode": "calm",
      "emotionEmoji": "🌿",
      "emotionLabel": "차분해",
      "memo": "사진 한 장 없이도 남길 수 있는 기록이에요.",
      "photoUrl": null
    },
    "partner": null
  },
  "bookProgress": {
    "lookbackDays": 30,
    "requiredDays": 20,
    "recordedDays": 1,
    "remainingDays": 19,
    "state": "growing"
  }
}
```

입력 규칙:

- `emotionCode`는 필수
- `memo`는 선택
- `photoUrl`은 선택
- 실제 R2 upload intent는 다음 슬라이스에서 추가한다

### 6.9 `GET /api/v1/book-snapshots/current`

목적:

- 책 탭이 성장 구간과 주문 가능 구간을 모두 같은 endpoint로 렌더링하게 한다.

응답:

```json
{
  "bookProgress": {
    "lookbackDays": 30,
    "requiredDays": 20,
    "recordedDays": 12,
    "remainingDays": 8,
    "state": "growing"
  },
  "candidateMoments": [
    {
      "id": "moment-cpl_active_20260407-2026-04-06",
      "localDate": "2026-04-06",
      "dateLabel": "4월 6일 월",
      "state": "complete",
      "me": {
        "author": "지우",
        "emotionCode": "happy",
        "emotionEmoji": "😊",
        "emotionLabel": "좋아",
        "memo": "산책이 좋았어요.",
        "photoUrl": "https://images.unsplash.com/photo-1511988617509-a57c8a288659?auto=format&fit=crop&w=1200&q=80"
      },
      "partner": {
        "author": "민준",
        "emotionCode": "loving",
        "emotionEmoji": "🥰",
        "emotionLabel": "다정해",
        "memo": "같이 본 장면이 오래 남았어요.",
        "photoUrl": null
      }
    }
  ],
  "snapshot": null,
  "order": null
}
```

규칙:

- `candidateMoments`는 최근 30일 창만 포함한다.
- archived 관계 snapshot/order는 여기 섞지 않는다.

---

## 7. 프론트 상태와의 매핑

| 프론트 개념 | 백엔드 source of truth |
|---|---|
| `relationshipState` | `relationship.state` |
| `coupleInfo` | `relationship` |
| `todayCardState` | `todayCard.state` |
| `moments` | `recentMoments` + day-card list 확장 API 예정 |
| `archiveMoments` | `archive.sections[].records` |
| `bookProgress` | `bookProgress` |
| `bookState` | `bookProgress.state` |
| `activeCycleId` | `relationship.coupleId` |

---

## 8. 구현 메모

### 8.1 현재 패키지 기준

```text
controller/
dto/
  archive/
  books/
  common/
  couples/
  daycard/
  home/
service/
entity/
repository/
support/
  error/
config/
```

원칙:

- controller는 DTO와 route만 책임진다.
- service는 현재 사용자 해석과 도메인 응답 조립을 맡는다.
- entity / repository는 `users`, `couples`, `day_cards`, `card_entries` core 모델을 담당한다.
- 공통 예외와 ProblemDetail 확장은 `support.error`에서 맡는다.

### 8.2 OpenAPI 노출

- `/v3/api-docs`
- `/swagger-ui.html`

이 두 경로는 프론트/백엔드 계약 확인용으로 초기부터 연다.

### 8.3 인증 토글

- 기본은 local 개발 모드에서 인증 비활성
- auth-disabled 모드에서는 `X-Today-Us-Auth-User-Id`로 현재 사용자를 바꿔가며 contract를 검증할 수 있다.
- 추후 `today-us.security.auth-enabled=true`로 Supabase JWT 검증 연결

---

## 9. 이번 라운드 밖

- `GET /api/v1/day-cards` 월별/범위 조회
- 이미지 업로드 intent 발급
- snapshot 생성/빌드/주문 쓰기 API
- 실제 archive pagination
- admin/ops surface
- Supabase JWT 검증, 운영 Postgres 연결

---

## 10. 최종 결론

이번 계약의 핵심 문장은 그대로다.

`현재 화면은 active couple이 책임지고, 이전 기록은 archived couple이 책임진다.`

지금 구현은 그 문장을 JPA 모델, 조회 쿼리, DTO 이름까지 포함해 먼저 고정한 상태다.
