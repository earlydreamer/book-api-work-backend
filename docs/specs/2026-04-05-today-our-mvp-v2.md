# 오늘 우리 MVP 명세 v2

작성일: 2026-04-05
상태: Draft

## 1. 제품 한 줄 정의

`오늘 우리`는 연인이 사진 1장과 감정 1개로 하루를 기록하고, 누적된 기록을 실제 책으로 주문할 수 있는 비공개 기록 서비스다.

## 2. 제품 목표

- 연인 간 일상 기록을 낮은 마찰로 쌓게 만든다.
- 만난 날뿐 아니라 각자 보낸 날도 유효한 기록으로 남긴다.
- 누적된 기록이 실제 주문 가능한 책으로 이어지게 만든다.

## 3. 대상 사용자

- 연인 2인
- 사진과 짧은 메모로 하루를 남기고 싶은 사용자
- 거창한 일기보다 가볍게 기록하고, 나중에 실물 책으로 받아보고 싶은 사용자

## 4. 핵심 가치

- 같이 있는 날도, 각자 보낸 날도 기록이 된다.
- 하루 기록이 쌓여 둘만의 아카이브가 된다.
- 기록이 실제 주문 가능한 책으로 변환된다.

## 5. 핵심 루프

1. 하루 카드가 열린다.
2. 한 사람 또는 두 사람이 오늘 기록을 남긴다.
3. 카드가 부분 카드 또는 완성 카드로 저장된다.
4. 기록이 누적되어 `30일 북` 후보가 생성된다.
5. 사용자가 책을 생성하고, 실제 주문한다.

## 6. 기록 규칙

- 기록 단위는 `1일 1카드`
- 하루 마감은 `새벽 4시`
- 과거 `3일`까지 수정 가능
- 카드 상태
  - 빈 카드
  - 부분 카드
  - 완성 카드
  - 마감 카드
- 부분 카드는 실패 상태가 아니라 유효한 기록으로 남긴다.

## 7. 기록 입력

- 입력 요소
  - 사진 1장 또는 무드 카드 1개
  - 감정 1개 필수
  - 짧은 메모 선택
- 작성 목표 시간: `20~30초`
- 입력 흐름
  1. 사진 선택
  2. 감정 선택
  3. 한 줄 메모
  4. 저장
- 사진이 없는 날은 무드 카드로 대체 가능

## 8. 주요 화면

### 8.1 홈

- 오늘 카드 상태
- 상대 기록 여부
- 어제 카드 요약
- 오늘 남기기 CTA

### 8.2 아카이브

- 날짜순 하루 카드 피드
- 월별 구분
- 부분 카드와 완성 카드 모두 노출

### 8.3 기록

- 중앙 CTA 기반 입력 화면

### 8.4 북

- 책 후보 상태
- 누적 기록 수
- 생성된 책 상태
- 주문 가능 전 미리보기
- 주문 진입

### 8.5 내 공간

- 우리 이름
- 시작일
- 알림 설정
- 연결 상태
- 프라이버시 설정

## 9. 온보딩

1. 가치 설명 3장
2. 계정 생성
3. 초대 코드 기반 커플 연결
4. 우리 이름, 시작일, 알림 시간 설정
5. 첫 기록 남기기

온보딩 종료 기준은 설명 완료가 아니라 `첫 카드 생성`이다.

## 9.1 관계 전환 정책

- 연결을 끊어도 이전 파트너와의 기록은 삭제하지 않는다.
- 이전 기록은 보관함에서 계속 확인할 수 있어야 한다.
- 다시 연결하면 현재 관계의 기록 수와 책 진행도는 `0일부터 다시 시작`한다.
- 피드는 언제나 현재 관계의 기록만 보여준다.
- 보관함은 `현재 연결 기록`과 `이전 연결 기록`을 구분해서 보여준다.

## 10. 알림 정책

- 아침 오픈 알림 1회
- 상대 기록 알림
- 카드 완성 알림
- 기념/책 알림은 주 1회 이하
- 하루 최대 알림은 기본 2개 이하
- 각자 로컬 시간대 기준
- 죄책감 유도 문구 금지

## 11. 첫 주문 상품

### 11.1 SKU

- 상품명: `오늘 우리 30일 북`
- 판형: `PHOTOBOOK_A4_SC`
- 페이지: `24p 고정`
- 포지션: 월간 다이어리형 기록책

### 11.2 주문 가능 조건

- 최근 30일 중 기록 20일 이상
- 여기서 `30일`은 캘린더 월이 아니라 `snapshot_created_at` 기준 최근 30개의 로컬 날짜를 의미한다.
- 한 번 생성된 스냅샷은 해당 30일 구간을 고정하며, 이후 카드 수정은 기존 주문 후보를 바꾸지 않는다.

### 11.3 가격 정책

- 현재 가격은 확정하지 않는다.
- 목표 가격대는 `19,900 ~ 24,900원`
- 실제 판매가는 `A4 24p 실제 원가 확인 후 확정`

### 11.4 주문 단순화 정책

- MVP에서는 `1회 주문 = 1권 = 1개 배송지`로 제한한다.
- 장바구니, 다중 책 동시 주문, 쿠폰, 수량 선택은 제외한다.
- 재주문 기능은 열지 않는다. 같은 기간을 다시 주문하려면 새 스냅샷을 생성한다.

### 11.5 첫 SKU 선택 이유

- `30일 상품`과 페이지 최소 규칙이 잘 맞는다.
- `SQUAREBOOK_HC`보다 프리미엄 부담이 낮다.
- 일기장/알림장 계열 상품 톤과 잘 맞는다.
- MVP 단계에서 책 생성 로직을 단순하게 유지할 수 있다.

## 12. 책 구조

- 오프닝 `2p`
- 본문 `20p`
- 엔딩 `2p`

### 12.1 본문 배치 규칙

- 기본은 `1일 1p`
- 기록일이 20일 초과하면 아래 우선순위로 `2일 1p`로 압축한다.
  1. 부분 카드
  2. 무드 카드
  3. 메모가 짧고 사진 밀도가 낮은 완성 카드
- 기록일이 20일 미만이면 주문 불가

### 12.2 MVP 템플릿 구성

- 표지 1종
- 오프닝 1종
- 하루 1p 기본형 1종
- 하루 1p 부분형 1종
- 엔딩 1종

MVP에서는 `하이라이트 2p 템플릿`을 제외한다.

## 13. 책 생성 방식

MVP는 `스냅샷 기반 비동기 생성`으로 간다.

### 13.1 흐름

1. 사용자가 `책 만들기`를 누른다.
2. 우리 서버가 `book_snapshot`을 생성한다.
3. 스냅샷 기준으로 페이지를 계산한다.
4. 백엔드 잡이 Sweetbook 책 생성 API를 호출한다.
5. 최종화 성공 시 주문 가능 상태로 전환한다.
6. 사용자가 배송지를 입력하고 주문한다.

### 13.2 book_snapshot에 포함해야 할 정보

- 포함 카드 목록
- 카드별 상태
- 표지 대표 사진
- 책 제목
- 기간
- 템플릿 버전
- 생성 시점 가격 기준

스냅샷 생성 이후에는 주문본 기준 데이터가 흔들리지 않아야 한다.

### 13.3 스냅샷 불변 규칙

- 스냅샷은 주문 후보의 불변 버전이다.
- 스냅샷 생성 이후 카드 수정, 삭제, 감정 변경은 기존 스냅샷에 반영하지 않는다.
- 사용자가 수정된 내용으로 책을 다시 만들고 싶으면 새 스냅샷을 생성해야 한다.
- 한 커플은 같은 30일 구간에 대해 `주문 가능 상태` 스냅샷을 하나만 유지한다.

## 14. Sweetbook API 연동 범위

### 14.1 반드시 포함

- 책 생성
- 사진 업로드
- 표지 생성
- 내지 생성
- 최종화
- 주문 생성
- 주문 상태 조회
- 충전금 잔액 확인
- 웹훅 설정 및 주문 상태 웹훅 수신

### 14.2 권장 처리

- 주문 생성 시 `Idempotency-Key` 필수
- 사진은 URL 직접 참조보다 업로드 후 `fileName` 참조 우선
- 웹훅 수신 시 `X-Webhook-Signature` 검증 필수
- `X-Webhook-Delivery` 기준 중복 이벤트를 무시할 수 있어야 한다.
- Sweetbook 연동은 공식 SDK 의존 없이 내부 REST client로 구현한다.
- 공식 SDK 저장소는 참고용으로만 보되, 실제 런타임에서는 사용하지 않는다.
- 이유는 내부 주문 ID 기반의 결정적 `Idempotency-Key`, Spring 런타임 통합, structured logging, timeout/retry 정책, webhook 헤더 검증 규칙을 우리 서비스가 직접 통제해야 하기 때문이다.

### 14.3 실제 플로우

1. `POST /v1/books`
2. `POST /v1/books/{bookUid}/photos`
3. `POST /v1/books/{bookUid}/cover`
4. `POST /v1/books/{bookUid}/contents` 반복
5. `POST /v1/books/{bookUid}/finalization`
6. `GET /v1/credits`
7. `POST /v1/orders`
8. `PUT /v1/webhooks/config`

### 14.4 내부 연동 원칙

- Sweetbook client는 우리 백엔드의 명시적 모듈로 둔다.
- 모든 쓰기 요청은 호출자가 `Idempotency-Key`를 직접 주입할 수 있어야 한다.
- Sweetbook 응답은 내부 표준 에러 모델로 정규화해 저장한다.
- 로그에는 `snapshot_id`, `order_id`, `sweetbook_book_uid`, `sweetbook_order_uid`를 함께 남긴다.
- webhook 검증 로직도 동일 모듈 안에서 관리한다.

## 15. 결제 및 운영 정책

- 유저는 우리 서비스에 일반 상품 결제를 한다.
- 우리는 Sweetbook 충전금을 원가 지갑처럼 운영한다.
- 유저 결제와 Sweetbook 충전금은 1:1로 연결하지 않는다.
- 고객 결제 provider는 `Toss Payments 결제위젯 v2`로 고정한다.
- 프론트는 주문서 페이지 안에서 Toss 결제 UI와 약관 UI를 렌더링한다.
- 프론트는 `requestPayment()` 호출 시 `successUrl`, `failUrl`를 사용한다.
- `successUrl` 복귀 후 서버는 `paymentKey`, `orderId`, `amount`, `paymentType`를 검증하고 내부 주문 정보와 일치할 때만 Toss `POST /v1/payments/confirm`을 호출한다.
- MVP 1차 결제는 `NORMAL` 일회성 결제를 기준으로 설계한다.
- Toss 결제 confirm은 인증 완료 후 10분 안에 수행되어야 한다.
- 인증은 `Supabase Auth`를 사용한다.
- 인증 구현은 `Spring Security + stateless JWT Bearer`를 기준으로 한다.
- 백엔드는 `Spring Boot 4.0.3` 기반 layered architecture로 구현한다.
- 인가는 클라이언트가 아니라 서버 측 `Spring API`가 강제한다.
- JWT는 `Authorization: Bearer <token>` 형태로 전달하고, 서버는 `iss`, `aud`, `exp`, `nbf`를 검증한다.
- 온프레미스 배포는 `React 프론트엔드 + Spring Boot API` 기준으로 설계한다.
- public 앱은 `bookservice.earlydreamer.dev`, admin 앱은 `bookservice-admin.earlydreamer.dev`로 분리한다.
- 각 서브도메인은 Cloudflare Tunnel로 origin에 연결한다.
- 프론트 인증 클라이언트는 `Supabase JS (PKCE)`를 사용한다.
- public/admin은 서로 다른 origin이므로 세션 공유를 전제하지 않고, admin은 별도 로그인 surface로 취급한다.
- 프론트는 `Supabase JS`의 auth state change를 기준으로 세션을 추적하고, 임의의 localStorage 값을 권한 source of truth로 사용하지 않는다.
- admin과 운영 액션은 public 앱 내부 경로가 아니라 별도 admin 서브도메인에서만 노출한다.
- admin API는 `OPS_ADMIN` 권한과 allowlist 2차 검증이 필요하다.
- admin API는 admin host에서만 노출하고, 경로도 public API와 분리된 `ops` surface로 관리한다.
- public/admin 모두 강한 CSP를 적용하고 inline script 의존을 피한다.
- webhook endpoint는 사용자 인증이 아니라 provider별 verification material 검증과 delivery dedupe로만 통과시킨다.
- 결제 provider는 내부 `PaymentGateway` 경계 뒤에 두되 첫 구현은 `TossPaymentsGateway`로 한다.
- Toss webhook은 결제 성공의 1차 트리거가 아니라 reconciliation과 비동기 결제수단 확장을 위한 보조 경로로 둔다.
- 주문 전 잔액 확인을 수행한다.
- 저잔액 운영 알림이 필요하다.
- 판매가는 Sweetbook `book-spec` 실원가와 배송비를 기준으로 내부 가격표에서 관리한다.
- Sweetbook 문서는 `POST /orders/estimate` 사용을 안내하지만, 현재 공개 Orders 페이지에 전용 섹션이 없으므로 MVP는 해당 API 의존 없이 내부 가격표로 운영한다.
- 주문 실패 후 환불이 필요한 경우를 대비해 `결제 성공 -> Sweetbook 주문 실패` 보상 흐름을 반드시 둔다.
- 운영자 화면에서 최소한 아래를 볼 수 있어야 한다.
  - credits 잔액
  - build 실패 snapshot
  - `payment_succeeded`지만 `order_failed`인 주문
  - 최근 운영자 액션 audit log

## 16. 주문 원자성 정책

아래 상황에서 `유저는 결제했는데 주문이 없는 상태`가 발생하면 안 된다.

- Sweetbook 최종화 실패
- Sweetbook 잔액 부족
- 주문 생성 실패
- 중복 클릭
- 네트워크 오류

따라서 내부 주문 흐름은 아래 상태를 명시적으로 가져야 한다.

- payment_pending
- payment_succeeded
- payment_refund_required
- payment_refunded
- snapshot_created
- build_requested
- build_succeeded
- build_failed
- order_requested
- order_succeeded
- order_failed

주문 실패 시 `payment_succeeded` 상태가 남아 있으면 자동 환불 또는 운영 개입 대상이 되어야 한다.

## 17. 예외 상황 정책

MVP에서도 아래는 최소한 정의가 필요하다.

- 커플 연결 해제
- 초대 코드 만료 또는 취소
- 주문 이후 기록 수정 불가 기준
- 헤어짐 또는 한쪽 탈퇴 시 기록 접근권
- 주문 취소 가능 범위
- 배송지와 수령인 정보는 주문 이행 범위에서만 사용하고, 로그에는 전체 값을 남기지 않는다.
- 주문 상태는 웹훅 기반으로 동기화하고, 사용자가 상세 화면을 열 때 최신 상태를 재조회한다.

## 18. MVP에서 제외

- 공개 SNS 요소
- 댓글 스레드/채팅
- 자유 편집기
- AI 문구 생성
- 친구/가족 확장
- 감정 리포트
- 캘린더 뷰
- 다중 SKU
- 프리미엄 하드커버 상품
- 하이라이트 2p 레이아웃

## 19. 다음 단계

이 문서를 기준으로 다음 산출물이 필요하다.

1. `frontend/book-api-work-frontend/DESIGN.md`
2. public 홈 layout blueprint
3. 프론트 화면/route inventory
4. 기능 목록 상세화
5. 데이터 모델 정의
6. 주문 상태 머신 정의
7. Sweetbook API 매핑 표
8. 구현 순서 계획

구현 원칙:
- 저장소는 `frontend`와 `backend` 두 개만 유지한다.
- public/admin은 별도 surface지만 모두 `frontend` 저장소 안에서 관리한다.
- 초기 구현은 `디자인 시스템과 레이아웃 선행`을 따른다.
- 즉, `DESIGN.md -> layout shell -> mock 화면 -> typed contract -> 실제 API 연동` 순서로 간다.
- 디자인 기준이 문서화되기 전에는 새로운 화면 확장을 진행하지 않는다.

## 20. 외부 제약 요약

현재 공개 문서 기준으로 확인한 제약:

- `PHOTOBOOK_A4_SC`: `24 ~ 130p`
- `PHOTOBOOK_A5_SC`: `50 ~ 200p`
- `SQUAREBOOK_HC`: `24 ~ 130p`
- 주문은 `FINALIZED` 책만 가능
- 주문 시 파트너 충전금이 즉시 차감
- 배송비는 주문당 `3,000원`
- Live 충전은 파트너 포털에서 수행

참고 문서:

- https://api.sweetbook.com/docs/api/book-specs/
- https://api.sweetbook.com/docs/api/books/
- https://api.sweetbook.com/docs/api/templates/
- https://api.sweetbook.com/docs/api/orders/
- https://api.sweetbook.com/docs/api/credits/
