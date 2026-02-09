#!/bin/bash
# Server Setup Script for Ubuntu 22.04/24.04
# Run this script on the Lightsail instance

set -e

echo "=== SellSync Server Setup ==="

# Update system
echo "[1/6] Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Java 21
echo "[2/6] Installing Java 21..."
sudo apt install -y openjdk-21-jdk

# Verify Java installation
java -version

# Install Redis
echo "[3/6] Installing Redis..."
sudo apt install -y redis-server
sudo systemctl enable redis-server
sudo systemctl start redis-server

# Create application directory
echo "[4/6] Creating application directories..."
sudo mkdir -p /opt/sellsync
sudo mkdir -p /var/log/sellsync
sudo chown -R ubuntu:ubuntu /opt/sellsync
sudo chown -R ubuntu:ubuntu /var/log/sellsync

# Copy systemd service file
echo "[5/6] Setting up systemd service..."
sudo cp /opt/sellsync/sellsync.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable sellsync

# Configure firewall
echo "[6/6] Configuring firewall..."
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 8080/tcp  # Spring Boot (optional, remove in production)
sudo ufw --force enable

echo ""
echo "=== Setup Complete ==="
echo "Next steps:"
echo "1. Copy .env file to /opt/sellsync/.env and configure"
echo "2. Copy app.jar to /opt/sellsync/app.jar"
echo "3. Run: sudo systemctl start sellsync"
echo "4. Check status: sudo systemctl status sellsync"
echo "5. View logs: sudo journalctl -u sellsync -f"
