# Today Us — Design System

> 참고 사본: frontend repo의 `DESIGN.md`를 backend handoff용으로 복제한 문서다.

작성일: 2026-04-07
상태: 현행 앱(`today-us-front`) 기준 정리 · Approved

---

## 1. 디자인 한 줄 정의

`오늘 우리`는 커플을 위한 **private stories feed**다.
둘만 보는 사적인 공간이고, 기록이 쌓이면 실물 책으로 이어진다.

---

## 2. 기술 스택 및 의존성

| 항목 | 선택 |
|---|---|
| 스타일링 | Tailwind CSS v4 (`@import "tailwindcss"`) |
| 디자인 토큰 | `@theme {}` 블록에 CSS Custom Properties로 정의 |
| 애니메이션 | `motion/react` (`motion/react` 패키지) |
| 아이콘 | `lucide-react` |
| 폰트 | Plus Jakarta Sans (headline), Be Vietnam Pro (body), Pretendard (한국어 fallback) |

---

## 3. 컬러 시스템

### 3.1 기준 원칙

- Primary accent는 **coral/orange** 단일 계열
- 배경은 `surface-container-lowest` (white에 가까운 warm white)
- 텍스트는 `on-surface` (warm dark gray)
- Secondary는 warm red 계열, Tertiary는 purple 계열 (강조 보조로만 사용)

### 3.2 핵심 토큰 (index.css `@theme` 블록 기준)

```css
/* Primary — coral/orange */
--color-primary: #a03a0f;
--color-primary-container: #fe7e4f;
--color-primary-fixed: #fe7e4f;
--color-inverse-primary: #fe7e4f;
--color-on-primary: #ffefeb;
--color-on-primary-container: #491300;

/* Surface */
--color-background: #f5f6f7;
--color-surface: #f5f6f7;
--color-surface-bright: #f5f6f7;
--color-surface-container-lowest: #ffffff;
--color-surface-container-low: #eff1f2;
--color-surface-container: #e6e8ea;
--color-surface-container-high: #e0e3e4;
--color-surface-container-highest: #dadddf;
--color-surface-dim: #d1d5d7;

/* Text */
--color-on-surface: #2c2f30;
--color-on-surface-variant: #595c5d;
--color-outline: #757778;
--color-outline-variant: #abadae;
```

### 3.3 사용 규칙

- `bg-primary` / `coral-gradient` → CTA 버튼, 강조 액션
- `bg-surface-container-lowest` (white) → 카드 배경
- `bg-surface-container-low` → input field, secondary surface
- `text-on-surface` → 본문, `text-on-surface-variant` → 부제목/메타
- Secondary/Tertiary는 보조 강조에만 (partner 이름, Soulmates badge 등)

---

## 4. 타이포그래피

### 4.1 폰트 패밀리

```css
--font-headline: "Plus Jakarta Sans", "Pretendard", sans-serif;
--font-body: "Be Vietnam Pro", "Pretendard", sans-serif;
```

- `font-headline` → 페이지 제목, 카드 날짜, 브랜드 워드마크
- `font-body` (기본) → 모든 본문, UI 텍스트

### 4.2 스케일

| 역할 | 클래스 예시 | 용도 |
|---|---|---|
| Brand | `text-xl font-black font-headline` | 헤더 워드마크 |
| Page title | `text-4xl font-black font-headline` | 페이지 상단 제목 |
| Hero | `text-5xl md:text-7xl font-black font-headline` | Landing hero |
| Card title | `text-lg font-black font-headline` | MomentCard 날짜 |
| Body | `text-sm leading-relaxed` | 기록 텍스트 |
| Meta | `text-[10px] font-bold uppercase tracking-wider` | 상태 뱃지 |

---

## 4.5 UX 라이팅

기준 참고:
- 토스 UX 라이팅 가이드: `https://developers-apps-in-toss.toss.im/design/ux-writing.html`

원칙:
- 앱 안의 문구는 **차분하고 부드러운 `~해요.` 체**를 기본으로 사용
- 설명 문구는 사용자가 바로 이해할 수 있게 **짧고 능동형**으로 작성
- `없어요`, `안 돼요` 같은 부정형은 꼭 필요한 경우에만 사용하고, 가능하면 `~하면 할 수 있어요`처럼 긍정형으로 전환
- `하시겠어요`, `계시다`, `여쭙다` 같은 과한 경어는 피하고 캐주얼한 경어를 사용
- CTA는 압박하지 않고, 지금 할 수 있는 행동을 부드럽게 제안
- 빈 상태, 저장 완료, 에러, 확인 패널까지 같은 톤을 유지

금지:
- 반말 CTA (`해볼까`, `가자`, `남겨보자`)
- 과장된 감탄형 문구
- 영어 중심 상태 라벨
- 의미가 모호한 명사형 카피 (`기록 진행`, `연결 완료 상태`) 남발

예외:
- 정책상 불가, 종료, 만료, 사용자 영향 고지처럼 명확성이 우선인 경우에만 제한적으로 부정형/수동형 허용

---

## 5. 레이아웃 시스템

### 5.1 원칙

- **Single-spine feed**: public 본체는 단일 세로 축
- 모바일에서 1열, 데스크탑에서도 가능한 한 같은 축 유지
- 최대 폭: `max-w-3xl`(피드) ~ `max-w-7xl`(대시보드 섹션)
- 여백 리듬: `px-4 md:px-6`, `py-8 pb-32`

### 5.2 진입 순서

```
/ 랜딩
  → /signup 회원가입
  → /connect 커플 연결
  → /feed 피드 (메인)
  → /record 기록 작성
  → /archive 아카이브
  → /make-book 책 만들기
  → /me 내 공간
```

---

## 6. 컴포넌트 원칙

### 6.1 Card

- 카드 배경: `bg-white rounded-2xl shadow-sm border border-outline-variant/10`
- 호버: `hover:shadow-md transition-all duration-300`
- 강한 카드(landing hero): `shadow-2xl shadow-primary/5`

### 6.2 Button

```
Primary CTA    : coral-gradient text-white rounded-full px-10 py-4 font-bold shadow-lg shadow-primary/20
Secondary      : border-2 border-outline-variant/30 text-on-surface-variant rounded-full
Compact        : bg-primary text-white rounded-full px-4 py-1.5 text-xs font-black
Ghost/Text     : text-primary font-bold hover:underline
```

- 강한 primary CTA는 한 화면에 하나
- 버튼 호버: `hover:scale-[0.98]`, 탭: `active:scale-95`
- pill 형태(`rounded-full`)가 기본, 폼 내부는 `rounded-xl`

### 6.3 Input

```
기본: bg-surface-container-low rounded-xl py-4 px-5 border-2 border-transparent focus:border-primary/30 focus:outline-none transition-all
아이콘 포함: pl-12 (left icon at left-4 top-1/2)
```

### 6.4 Navigation

**데스크탑 (Top nav)**
- `sticky top-0 bg-white/80 backdrop-blur-md border-b border-outline-variant/10 h-16`
- 링크: `text-sm font-bold`, active: `text-primary`, inactive: `text-on-surface-variant hover:text-primary`

**모바일 (Bottom nav)**
- `fixed bottom-0 bg-white/90 backdrop-blur-xl border-t border-outline-variant/10 px-6 py-3`
- Record 버튼: FAB 스타일, `-mt-10`, `w-14 h-14 bg-primary rounded-full`

### 6.5 Toggle

```
켜짐: bg-primary (w-11 h-6 rounded-full)
꺼짐: bg-surface-container-highest
thumb: w-5 h-5 bg-white rounded-full, translate-x-0 / translate-x-5
```

---

## 7. 모션 원칙

라이브러리: `motion/react`

| 패턴 | 설정 |
|---|---|
| 페이지 진입 | `initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}` |
| 카드 스택 | `whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}` |
| 스텝 전환 | `initial={{ opacity: 0, x: 20 }} exit={{ opacity: 0, x: -20 }}` |
| 버튼 hover | `hover:scale-[0.98] active:scale-95` (Tailwind, motion 아님) |
| FAB/이미지 | `hover:scale-105 hover:rotate-0 transition-transform` |

- `AnimatePresence mode="wait"` → 스텝 전환, 상태 전환
- `whileInView` → 피드 카드 lazy 등장

---

## 8. 유틸리티 클래스 (index.css 정의)

```css
.coral-gradient   /* bg: linear-gradient(135deg, #a03a0f 0%, #fe7e4f 100%) */
.editorial-shadow /* box-shadow: 0 12px 40px rgba(44, 47, 48, 0.06) */
.no-scrollbar     /* 스크롤바 숨김 */
```

---

## 9. 상태 UX 원칙

| 상태 | 처리 방식 |
|---|---|
| Empty | 온보딩 CTA (빈 피드 = 첫 기록 시작 안내) |
| Loading | 버튼 내 spinner (`animate-spin border-t-white`) |
| Partial | PARTIAL 카드 = "기록 기간 지남" / "파트너를 기다림" |
| Complete | 두 사람 모두 기록 = Merged View (Soulmates Linked) |
| Error | 인라인 에러 메시지 (red text, 필드 아래) |
| Saved | 완료 체크 + 카드 미리보기 + 피드 이동 CTA |

### 9.1 관계 기록 표시 정책

- 피드는 언제나 `현재 연결`의 기록만 보여준다.
- 새 관계가 시작된 뒤에는 이전 관계 기록이 피드에 섞여 보이면 안 된다.
- 연결을 끊어도 이전 기록은 삭제하지 않고 보관함으로 이동한다.
- 다시 연결하면 현재 관계의 기록 수와 책 진행도는 `0일부터 다시 시작`한다.
- 보관함은 `현재 연결 기록`과 `이전 연결 기록`을 별도 섹션으로 나눈다.
- 이전 연결 카드에는 `이전 파트너와의 기록` 라벨을 붙인다.
- 부분 카드의 레이아웃은 완성 카드와 같은 2열 구조를 유지한다.
- 지난 날짜의 부분 카드는 기다림 CTA 없이 읽기 전용 상태로만 보여준다.

---

## 10. 금지 패턴

- cold blue-gray 메인 팔레트
- decorative icon circles (의미 없는 원형 아이콘 장식)
- 균일 높이 카드 모자이크 (SaaS dashboard 느낌)
- 공개 SNS 좋아요/댓글/조회수 톤
- 한 화면에 동급 강도의 CTA 2개 이상

---

## 11. 구현 순서 (현재 기준)

```
✅ Design token & index.css
✅ Landing
✅ Login / Signup
✅ Feed (mock data)
✅ Archive (mock data)
✅ MakeBook (step wizard)
✅ MomentCard (3 states)
✅ Layout (Header + Bottom Nav)
✅ AuthContext (isConnected 포함)
✅ Connect (/connect)
✅ Me (/me)
✅ Record (기능화)
[ ] API 계약 정의 → mock adapter
[ ] 실제 백엔드 연동
```
