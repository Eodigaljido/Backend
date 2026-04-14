#!/bin/bash
# EC2 초기 설치 스크립트 (Amazon Linux 2023 기준)
# 사용법: bash setup-ec2.sh

set -e

REPO_URL="https://github.com/Eodigaljido/Backend.git"
APP_DIR="$HOME/Backend"

echo "=== [1/5] Docker 설치 ==="
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
echo "Docker 설치 완료: $(docker --version)"

echo "=== [2/5] Docker Compose 설치 ==="
COMPOSE_VERSION="v2.27.0"
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
echo "Docker Compose 설치 완료: $(docker-compose --version)"

echo "=== [3/5] 레포지토리 클론 ==="
if [ -d "$APP_DIR" ]; then
  echo "이미 클론됨. git pull 실행."
  cd "$APP_DIR" && git pull origin main
else
  git clone "$REPO_URL" "$APP_DIR"
fi

echo "=== [4/5] .env.prod 설정 ==="
if [ ! -f "$APP_DIR/.env.prod" ]; then
  cp "$APP_DIR/.env.example" "$APP_DIR/.env.prod"
  echo ""
  echo "⚠️  .env.prod 생성됨. 아래 명령어로 실제 값 입력 필수:"
  echo "    nano $APP_DIR/.env.prod"
  echo ""
  echo "특히 아래 항목 반드시 변경:"
  echo "  DB_HOST     → RDS 엔드포인트"
  echo "  REDIS_HOST  → ElastiCache 엔드포인트"
  echo "  JWT_SECRET  → openssl rand -base64 32"
  echo ""
else
  echo ".env.prod 이미 존재. 스킵."
fi

echo "=== [5/5] 완료 ==="
echo ""
echo ".env.prod 설정 후 아래 명령어로 앱 실행:"
echo "  cd $APP_DIR"
echo "  docker-compose -f docker-compose.prod.yml up -d --build"
echo ""
echo "로그 확인:"
echo "  docker-compose -f docker-compose.prod.yml logs -f app"
echo ""
echo "*** 주의: 이 세션을 재접속 후에 docker 명령어가 sudo 없이 동작합니다 ***"
