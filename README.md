# 오늘 우리 (Today Us) - 최종 제출 통합 가이드

> [!TIP]
> **시연 주소**: [https://today-us.earlydreamer.dev/](https://today-us.earlydreamer.dev/)  
> **백엔드 저장소**: [book-api-work-backend](./) | **프론트엔드 저장소**: [today-us-front](../../today-us/today-us-front)

---

## 1. 서비스 소개 🧔‍♂️📖
### "디지털 홍수 속에서 손에 잡히는 우리만의 기록"
**오늘 우리 (Today Us)**는 매일 쏟아지는 디지털 사진과 조각 일기를 모아, 세상에 하나뿐인 **실물 포토북**으로 엮어주는 서비스입니다. 

- **누구를 위한 서비스인가?**
  - 매일의 소중한 순간을 디지털 데이터로만 남기기 아쉬운 커플.
  - 아이의 성장 과정을 기록하고 나중에 선물하고 싶은 가족.
- **핵심 기능**
  - **감성 타임라인**: 파트너와 공유하는 프라이빗 일기 및 사진 피드.
  - **지능형 아카이빙**: 관계가 종료되어도 기록은 소중히 보관하며, 새로운 관계에서는 '0일'부터 다시 시작하는 유연한 관계 관리.
  - **실물 포토북 주문**: 스윗북(Sweetbook) API를 통한 실시간 포토북 제작 및 주문.
  - **안전한 보안**: Supabase Auth와 Cloudflare Tunnel 기반의 제로 트러스트(Zero Trust) 배포.

---

## 2. 기술 선택의 이유 및 설계 의도 ⚙️🏗️
### 왜 이 서비스를 선택했는가?
단순한 사진첩은 많습니다. 하지만 **'기록이 실물이 되는 순간'**의 감동은 다릅니다. 우리는 사용자에게 '소장 가치'가 있는 경험을 제공하기 위해 Printing-as-a-Service 모델을 MVP로 선택했습니다.

### 기술적 결정 (Technical Context)
- **Spring Boot 3 & Java 21**: 강력한 타입 안정성과 비동기 클라이언트(WebClient) 활용을 통한 외부 API(스윗북) 연동 최적화.
- **React & Vite**: 빠른 개발 피드백 루프와 현대적인 UI 컴포넌트 라이브러리 활용.
- **Supabase & Postgres**: 서버리스 인증과 강력한 RDBMS 기능을 통합하여 개발 속도 극대화.
- **Cloudflare Tunnel & Docker**: 공인 IP 노출 없이 시놀로지 NAS 내부망에 안전하게 서비스를 배포하는 터널링 인프라 구축.

---

## 3. 실행 방법 (How to Run) 🚀
### ⚙️ 공통 환경 변수 설정
각 폴더의 `.env.example`을 복사하여 실제 키값을 입력해 주세요.
- **프론트엔드**: `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, `VITE_API_BASE_URL`
- **백엔드**: `TODAY_US_DB_URL`, `TODAY_US_R2_ACCESS_KEY_ID`, `TODAY_US_SWEETBOOK_API_KEY` 등

### 🎨 프론트엔드 (Frontend)
```bash
cd today-us/today-us-front
npm install
npm run dev
```

### ⚙️ 백엔드 (Backend)
```bash
cd backend/book-api-work-backend
./gradlew bootRun
```

---

## 4. 사용한 API 목록 (Sweetbook Integration) 🔨
우리는 **Sweetbook Book Print API v1**을 활용하여 제작부터 배송까지의 파이프라인을 구축했습니다.

| API 엔드포인트 | 용도 | 핵심 구현 디테일 |
| :--- | :--- | :--- |
| `POST /v1/books` | 포토북 초안 생성 | **Idempotency-Key**를 통한 중복 생성 방지 |
| `POST /v1/books/{id}/photos` | 사진 자산 업로드 | Multipart Form Data 스트리밍 처리 |
| `POST /v1/books/{id}/cover` | 표지 레이아웃 생성 | 템플릿 파라미터 기반 자동 렌더링 |
| `POST /v1/books/{id}/contents` | 내지 데이터 주입 | 페이지 분할 로직 및 데이터 매핑 |
| `POST /v1/books/{id}/finalization` | 제작 확정 | 최종 페이지 수 산출 및 주문 준비 완료 |
| `POST /v1/orders` | 실물 주문 생성 | 배송지 정보 및 외부 참조 ID(External Ref) 연동 |

---

## 5. AI 도구 사용 내역 (AI Collaborative Development) 🤖🤝
본 프로젝트는 현대적인 AI 에이전트 워크플로우를 적극 수용하여 린하게 개발되었습니다.

- **Antigravity (Google DeepMind)**: 메인 아키텍트. 전체 시스템 아키텍처 설계, 보안 설정(Spring Security), CI/CD 배포 파이프라인 구축, 인증 시스템 고도화를 주도함.
- **Google AI Studio (Gemini 1.5 Pro)**: 초기 UI/UX 디자인 가이드 수립 및 토스 스타일의 차분한 경어체 라이팅 톤 정립.
- **Claude Code**: 도메인 로직 리팩토링 및 복잡한 인프라 설정 디버깅 지원.

---

## 6. 설계 의도 및 비즈니스 비전 🌟
- **비즈니스 가능성**: 개인화된 디지털 굿즈 시장은 해마다 성장 중입니다. "오늘 우리"는 단순히 사진을 저장하는 클라우드를 넘어, 물리적인 추억을 배송하는 매개체로서의 비즈니스 가치를 지독하게 파고듭니다.
- **확장 계획**: 
  - **AI 자동 선별**: 업로드된 사진 중 베스트 샷을 골라 자동으로 책을 구성하는 기능.
  - **정기 구독**: 매달 일정량 이상의 일기가 쌓이면 자동으로 미니 포토북을 발송하는 구독 모델.
  - **결제 연동**: Portone API를 통한 실제 결제 프로세스 연동 완료 예정.

---

> [!IMPORTANT]
> 본 프로젝트는 **'보관하는 기록'에서 '만져지는 추억'으로**라는 철학을 실현하기 위해 AI와 사람이 긴밀하게 협업하여 완성되었습니다. 🧔‍♂️🚀✨🎯
