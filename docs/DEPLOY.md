# 🚀 배포 가이드 (DEPLOY.md)

Docker Compose와 GitHub Actions를 활용하여 실서버에 백엔드를 배포하는 방법입니다. 🧔‍♂️🤘

## 1. GitHub Secrets 설정

GitHub 레포지토리의 `Settings > Secrets and variables > Actions`에 아래 항목들을 등록해야 합니다.

### 🔑 환경 변수 (Spring Boot)

`.env.local`의 값들을 기반으로 다음 이름들로 등록하세요.

- `TODAY_US_DB_URL`: JDBC 연결 문자열
  - 예시: `jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres?sslmode=require`
  - `user=`나 `password=`를 URL query에 넣지 말고, 아래 두 필드로 분리해요.
- `TODAY_US_DB_USERNAME`: DB 사용자명
- `TODAY_US_DB_PASSWORD`: DB 비밀번호
- `TODAY_US_DB_DRIVER`: DB 드라이버 (`org.postgresql.Driver`)
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI`: Supabase JWK URL
- `TODAY_US_SUPABASE_PROJECT_URL`: Supabase URL
- `TODAY_US_SECURITY_ALLOWED_ORIGINS`: CORS 허용 오리진
- `TODAY_US_SECURITY_ALLOWED_ORIGIN_PATTERNS`: 추가 허용 오리진 패턴
- `TODAY_US_R2_ACCOUNT_ID`: Cloudflare R2 계정 ID
- `TODAY_US_R2_ACCESS_KEY_ID`: R2 Access Key
- `TODAY_US_R2_SECRET_ACCESS_KEY`: R2 Secret Key
- `TODAY_US_R2_BUCKET`: R2 버킷명
- `TODAY_US_R2_PUBLIC_BASE_URL`: R2 공개 URL
- `TODAY_US_R2_UPLOAD_PREFIX`: 업로드 prefix
- `TODAY_US_R2_PRESIGN_TTL_SECONDS`: presign TTL
- `TODAY_US_SWEETBOOK_BASE_URL`: 스윗북 API 베이스 URL
- `TODAY_US_SWEETBOOK_API_KEY`: 스윗북 API 키
- `TODAY_US_SWEETBOOK_BOOK_SPEC_ID`: 스윗북 책 사양 ID
- `TODAY_US_SWEETBOOK_TEMPLATE_ID`: 스윗북 템플릿 ID
- `TODAY_US_SWEETBOOK_WEBHOOK_SECRET`: 스윗북 웹훅 비밀키

### 🛰️ 서버 접속 정보 (SSH / Cloudflare Tunnel)

- `SSH_HOST`: Cloudflare Tunnel에 등록된 도메인 (예: `ssh.today-us.com`)
- `SSH_PORT`: SSH 접속 포트 (보통 22, 시놀로지 변경 포트 등)
- `SSH_USERNAME`: 서버 접속 계정
- `SSH_PRIVATE_KEY`: 서버 접속용 개인키 (`id_rsa` 내용 전체)
- `CF_CLIENT_ID`: Cloudflare Zero Trust 서비스 토큰 ID
- `CF_CLIENT_SECRET`: Cloudflare Zero Trust 서비스 토큰 Secret
- `GHCR_TOKEN`: **GitHub Personal Access Token** (read:packages 권한 필수, Private 이미지용)

### 📂 경로 설정 (시놀로지 NAS 전용)

- `SSH_DEPLOY_PATH`: **SSH 셸 기준** 실제 경로 (예: `/volume1/docker/today-us/today-us-backend`)
- `SCP_TARGET_PATH`: **SCP/SFTP 기준** 전송 경로 (예: `/docker/today-us/today-us-backend`)
  - **참고**: 일반 Ubuntu 환경에서는 두 경로가 같을 수도 있지만, 가상 루트가 설정된 환경에서는 반드시 구분해야 합니다.
  - **🚨 절대 주의**: 공개키(`.pub`)가 아닌 **개인키** 파일을 복사해야 합니다. ed25519 등 OpenSSH 최신 규격도 지원합니다.
  - **서버 설정**: 대상 서버의 `~/.ssh/authorized_keys`에는 해당 키의 **공개키(Public Key, .pub)**가 등록되어 있어야 합니다.

---

## 2. 서버 사전 준비 (One-time)

서버에 처음 접속했을 때 다음 작업이 필요합니다.

```bash
# Docker 및 Docker Compose 설치 (Ubuntu 기준)
sudo apt-get update
sudo apt-get install docker.io docker-compose -y
sudo usermod -aG docker $USER

# 프로젝트 폴더 생성
mkdir ~/today-us-backend && cd ~/today-us-backend

# docker-compose.yml과 nginx/nginx.conf를 서버에 미리 생성해두거나 
# 배포 스크립트에서 자동 복사되도록 설정할 수 있습니다.
```

## 3. 배포 프로세스

1. `main` 브랜치에 코드를 `push` 합니다.
2. GitHub Actions가 자동으로 Docker 이미지를 빌드하여 **GHCR**에 올립니다.
3. 빌드가 끝나면 서버에 접속하여 최신 이미지를 `pull` 받고 컨테이너를 재시작합니다.
4. **18765** 포트로 서비스가 가동되는지 확인합니다! 🤘🔥

---

> [!IMPORTANT]
> - 서버의 방화벽에서 **18765** 포트가 열려 있는지 반드시 확인해 주세요.
> - 배포 파이프라인은 서버에 `.env` 파일을 생성한 뒤 권한을 `600`으로 고정합니다. 회전 후에는 이전 파일이나 백업본을 남기지 마세요.
> - `TODAY_US_DB_URL`에는 `user=`나 `password=`를 넣지 마세요. 계정 정보는 `TODAY_US_DB_USERNAME`, `TODAY_US_DB_PASSWORD`로 분리해야 로그 재노출을 막을 수 있어요.
