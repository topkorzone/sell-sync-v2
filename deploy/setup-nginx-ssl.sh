#!/bin/bash
# Nginx + Let's Encrypt SSL Setup
# Usage: ./setup-nginx-ssl.sh your-domain.com

set -e

DOMAIN=${1:-"your-domain.com"}

echo "=== Nginx + SSL Setup for $DOMAIN ==="

# Install Nginx
echo "[1/5] Installing Nginx..."
sudo apt install -y nginx

# Install Certbot
echo "[2/5] Installing Certbot..."
sudo apt install -y certbot python3-certbot-nginx

# Create webroot directory
sudo mkdir -p /var/www/certbot

# Copy Nginx config (temporary HTTP-only for cert generation)
echo "[3/5] Configuring Nginx..."
sudo tee /etc/nginx/sites-available/sellsync << EOF
server {
    listen 80;
    server_name $DOMAIN;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

sudo ln -sf /etc/nginx/sites-available/sellsync /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx

# Obtain SSL certificate
echo "[4/5] Obtaining SSL certificate..."
sudo certbot --nginx -d $DOMAIN --non-interactive --agree-tos --email admin@$DOMAIN

# Setup auto-renewal
echo "[5/5] Setting up auto-renewal..."
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer

echo ""
echo "=== SSL Setup Complete ==="
echo "Your site is now available at https://$DOMAIN"
echo ""
echo "Certificate will auto-renew. Test renewal with:"
echo "  sudo certbot renew --dry-run"
