# 🚀 배포 가이드 (DEPLOY.md)

Docker Compose와 GitHub Actions를 활용하여 실서버에 백엔드를 배포하는 방법입니다. 🧔‍♂️🤘

## 1. GitHub Secrets 설정

GitHub 레포지토리의 `Settings > Secrets and variables > Actions`에 아래 항목들을 등록해야 합니다.

### 🔑 환경 변수 (Spring Boot)

`.env.local`의 값들을 기반으로 다음 이름들로 등록하세요.

- `TODAY_US_DB_URL`: JDBC 연결 문자열
- `TODAY_US_DB_USERNAME`: DB 사용자명
- `TODAY_US_DB_PASSWORD`: DB 비밀번호
- `TODAY_US_SUPABASE_PROJECT_URL`: Supabase URL
- `TODAY_US_R2_ACCESS_KEY_ID`: R2 Access Key
- `TODAY_US_R2_SECRET_ACCESS_KEY`: R2 Secret Key (외 기타 R2 설정들...)
- `TODAY_US_SWEETBOOK_API_KEY`: 스윗북 API 키

### 🛰️ 서버 접속 정보 (SSH)

- `SSH_HOST`: 배포할 서버의 IP 주소 또는 도메인
- `SSH_PORT`: SSH 접속 포트 (기본값 22, 변경된 경우 해당 포트)
- `SSH_USERNAME`: 서버 접속 계정 (예: `ubuntu`)
- `SSH_DEPLOY_PATH`: **서버 내부의 실제 전체 경로** (SSH 커맨드 실행용)
  - 예: `/volume1/docker/today-us/today-us-backend` (시놀로지 NAS 등)
- `SCP_TARGET_PATH`: **SCP/SFTP가 바라보는 가상 루트 경로** (파일 전송용)
  - 예: `/docker/today-us/today-us-backend`
  - **참고**: 일반 Ubuntu 환경에서는 두 경로가 같을 수도 있지만, 가상 루트가 설정된 환경에서는 반드시 구분해야 합니다.
- `SSH_PRIVATE_KEY`: 서버 접속용 **개인키(Private Key)** 내용 전체
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
> - `.env` 파일 관리에 주의하세요. (현재는 CI/CD에서 환경 변수를 직접 주입하는 방식을 권장합니다.)
