#!/bin/bash
set -e

# Install dependencies
apt-get update -y
apt-get install -y git curl

# Install Docker
curl -fsSL https://get.docker.com | sh
usermod -aG docker ubuntu

# Clone the application
mkdir -p /app
git clone ${repo_url} /app/MenuBank

# Write production .env
cat > /app/MenuBank/.env <<EOF
DB_USER=${db_user}
DB_PASSWORD=${db_password}
DB_NAME=${db_name}
EOF

# Deploy
cd /app/MenuBank
docker compose -f docker-compose-prod.yaml up -d --build
