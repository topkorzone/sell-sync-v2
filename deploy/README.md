# SellSync 배포 가이드

## 자동 배포 (GitHub Actions)

`main` 브랜치에 `backend/` 경로의 파일이 push되면 자동으로 배포됩니다.

### 1. GitHub Secrets 설정

GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

| Secret Name | 설명 | 예시 값 |
|------------|------|--------|
| `SERVER_IP` | Lightsail 인스턴스 IP | `54.180.135.117` |
| `SSH_PRIVATE_KEY` | SSH 개인키 전체 내용 | `-----BEGIN RSA PRIVATE KEY-----...` |

#### SSH 키 설정 방법

**방법 1: 새 SSH 키 생성 (권장)**
```bash
# 로컬에서 실행
ssh-keygen -t ed25519 -f ~/.ssh/sellsync-deploy -C "github-actions-deploy"

# 공개키를 서버에 등록
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ubuntu@54.180.135.117 \
  "echo '$(cat ~/.ssh/sellsync-deploy.pub)' >> ~/.ssh/authorized_keys"

# GitHub Secret에 개인키 등록
cat ~/.ssh/sellsync-deploy
# 이 내용 전체를 SSH_PRIVATE_KEY secret에 복사
```

**방법 2: Lightsail 기본 키 사용**
```bash
# Lightsail 콘솔에서 다운로드한 키 내용을 그대로 사용
cat ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem
# 이 내용 전체를 SSH_PRIVATE_KEY secret에 복사
```

### 2. 서버 초기 설정 (최초 1회)

```bash
# SSH 접속
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ubuntu@54.180.135.117

# 설정 스크립트 다운로드 및 실행
curl -O https://raw.githubusercontent.com/YOUR_REPO/main/deploy/setup-github-deploy.sh
chmod +x setup-github-deploy.sh
./setup-github-deploy.sh
```

또는 수동으로:
```bash
# Java 21 설치
sudo apt update
sudo apt install -y openjdk-21-jdk redis-server

# 디렉토리 생성
sudo mkdir -p /opt/sellsync /var/log/sellsync
sudo chown -R ubuntu:ubuntu /opt/sellsync /var/log/sellsync
```

### 3. 환경변수 설정

```bash
# 서버에서 실행
nano /opt/sellsync/.env
```

`.env` 파일 내용:
```env
# Database
DB_URL=jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?prepareThreshold=0
FLYWAY_DB_URL=jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres?prepareThreshold=0
DB_USERNAME=postgres.nnwcuhamxjqgmyzptbyb
DB_PASSWORD=실제_비밀번호

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Encryption
ENCRYPTION_KEY=실제_암호화키

# Supabase
SUPABASE_URL=https://nnwcuhamxjqgmyzptbyb.supabase.co
SUPABASE_JWT_SECRET=실제_JWT_시크릿
SUPABASE_SERVICE_ROLE_KEY=실제_서비스롤키

# CORS
CORS_ALLOWED_ORIGINS=https://your-domain.com

# CJ API
CJ_API_BASE_URL=https://dxapi.cjlogistics.com:5004
```

### 4. 배포 테스트

```bash
# 로컬에서 main 브랜치에 push
git add .
git commit -m "Test deployment"
git push origin main
```

GitHub Actions 탭에서 워크플로우 실행 확인

---

## 수동 배포

```bash
# 로컬에서 실행
./deploy/deploy.sh 54.180.135.117 ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem
```

---

## 서버 관리 명령어

```bash
# 서비스 상태 확인
sudo systemctl status sellsync

# 서비스 재시작
sudo systemctl restart sellsync

# 실시간 로그 확인
sudo journalctl -u sellsync -f

# 애플리케이션 로그 확인
tail -f /var/log/sellsync/stdout.log

# Health check
curl http://localhost:8080/actuator/health
```

---

## 롤백

배포 실패 시 자동으로 롤백됩니다. 수동 롤백:
```bash
cd /opt/sellsync
mv app.jar.backup app.jar
sudo systemctl restart sellsync
```
