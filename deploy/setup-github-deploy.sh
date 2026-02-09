#!/bin/bash
# GitHub Actions 배포를 위한 서버 설정
# 서버(Lightsail)에서 실행

set -e

echo "=== GitHub Actions 배포 설정 ==="

# 1. 디렉토리 생성
echo "[1/4] Creating directories..."
sudo mkdir -p /opt/sellsync
sudo mkdir -p /var/log/sellsync
sudo chown -R ubuntu:ubuntu /opt/sellsync
sudo chown -R ubuntu:ubuntu /var/log/sellsync

# 2. Java 21 설치 확인
echo "[2/4] Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "Installing Java 21..."
    sudo apt update
    sudo apt install -y openjdk-21-jdk
fi
java -version

# 3. Redis 설치 확인
echo "[3/4] Checking Redis installation..."
if ! command -v redis-server &> /dev/null; then
    echo "Installing Redis..."
    sudo apt install -y redis-server
    sudo systemctl enable redis-server
    sudo systemctl start redis-server
fi

# 4. systemd 서비스 설정
echo "[4/4] Setting up systemd service..."
cat << 'EOF' | sudo tee /etc/systemd/system/sellsync.service
[Unit]
Description=SellSync Backend API
After=network.target

[Service]
Type=simple
User=ubuntu
Group=ubuntu
WorkingDirectory=/opt/sellsync
ExecStart=/usr/bin/java -jar -Xms512m -Xmx1536m -Dspring.profiles.active=prod /opt/sellsync/app.jar
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=10

EnvironmentFile=/opt/sellsync/.env

StandardOutput=append:/var/log/sellsync/stdout.log
StandardError=append:/var/log/sellsync/stderr.log

NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable sellsync

echo ""
echo "=== 설정 완료 ==="
echo ""
echo "다음 단계:"
echo "1. /opt/sellsync/.env 파일 생성 및 환경변수 설정"
echo "2. GitHub Secrets 설정 (README 참조)"
echo "3. main 브랜치에 push하여 배포 테스트"
