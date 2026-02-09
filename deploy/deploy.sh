#!/bin/bash
# Deployment Script - Run from local machine
# Usage: ./deploy.sh [server_ip] [ssh_key_path]

set -e

# Configuration
SERVER_IP=${1:-"54.180.135.117"}
SSH_KEY=${2:-"~/.ssh/lightsail-key.pem"}
SSH_USER="ubuntu"
REMOTE_PATH="/opt/sellsync"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== SellSync Deployment ==="
echo "Server: $SERVER_IP"
echo "Project: $PROJECT_ROOT"
echo ""

# Build JAR
echo "[1/4] Building JAR..."
cd "$PROJECT_ROOT/backend"
./gradlew :mh-api:bootJar -x test

# Find the built JAR
JAR_FILE=$(find mh-api/build/libs -name "*.jar" ! -name "*-plain.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "Error: JAR file not found!"
    exit 1
fi
echo "Built: $JAR_FILE"

# Upload JAR
echo ""
echo "[2/4] Uploading JAR to server..."
scp -i "$SSH_KEY" "$JAR_FILE" "$SSH_USER@$SERVER_IP:$REMOTE_PATH/app.jar.new"

# Upload service file (if needed)
echo "[3/4] Uploading service file..."
scp -i "$SSH_KEY" "$PROJECT_ROOT/deploy/sellsync.service" "$SSH_USER@$SERVER_IP:$REMOTE_PATH/"

# Deploy on server
echo ""
echo "[4/4] Deploying on server..."
ssh -i "$SSH_KEY" "$SSH_USER@$SERVER_IP" << 'ENDSSH'
    set -e
    cd /opt/sellsync

    # Backup current jar
    if [ -f app.jar ]; then
        cp app.jar app.jar.backup
    fi

    # Replace with new jar
    mv app.jar.new app.jar

    # Update systemd service if needed
    sudo cp sellsync.service /etc/systemd/system/
    sudo systemctl daemon-reload

    # Restart application
    sudo systemctl restart sellsync

    # Wait and check status
    sleep 5
    sudo systemctl status sellsync --no-pager
ENDSSH

echo ""
echo "=== Deployment Complete ==="
echo "Check logs: ssh -i $SSH_KEY $SSH_USER@$SERVER_IP 'sudo journalctl -u sellsync -f'"
