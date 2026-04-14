# EC2 서버 관리 명령어 모음

> EC2 접속: `ssh ec2-user@3.36.85.213`  
> 앱 디렉토리: `~/Backend`

---

## 앱 상태 확인

```bash
# 실행 중인 컨테이너 목록
sudo docker ps

# 앱 헬스체크
curl http://localhost:8080/actuator/health

# 컨테이너 상태 (Compose 기준)
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml ps
```

---

## 로그 확인

```bash
# 앱 실시간 로그 (스트리밍)
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml logs -f app

# 앱 로그 최근 100줄
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml logs --tail=100 app

# Redis 로그
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml logs --tail=50 redis

# 전체 컨테이너 로그
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml logs -f
```

---

## 앱 재시작

```bash
# 앱만 재시작
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml restart app

# 전체 재시작 (Redis + App)
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml restart

# 완전 중지 후 재시작
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml down && sudo docker compose -f docker-compose.prod.yml up -d

# 코드 변경 후 재빌드 + 재시작
cd ~/Backend && git pull origin main && sudo docker compose -f docker-compose.prod.yml up -d --build
```

---

## 앱 중지 / 시작

```bash
# 중지
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml down

# 시작
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml up -d

# 앱만 중지
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml stop app

# 앱만 시작
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml start app
```

---

## 환경변수 (.env.prod) 수정

```bash
# 파일 열기
nano ~/Backend/.env.prod

# 저장: Ctrl + O → Enter
# 종료: Ctrl + X

# 수정 후 앱 재시작 (환경변수 반영)
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml up -d --force-recreate app

# 현재 환경변수 확인 (민감정보 주의)
cat ~/Backend/.env.prod
```

---

## 최신 코드 배포 (수동)

```bash
cd ~/Backend

# 최신 코드 pull
git pull origin main

# 재빌드 + 재시작
sudo docker compose -f docker-compose.prod.yml up -d --build

# 사용하지 않는 이미지 정리
sudo docker image prune -f
```

---

## MySQL 관리

```bash
# MySQL 접속 (비밀번호 입력)
mysql -u root -p

# 또는 비밀번호 직접 입력 (스크립트용)
mysql -u root -p'여기에비밀번호'

# DB 목록 확인
mysql -u root -p -e "SHOW DATABASES;"

# eodigaljido DB의 테이블 목록
mysql -u root -p -e "USE eodigaljido; SHOW TABLES;"

# MySQL 서비스 상태
sudo systemctl status mysqld

# MySQL 재시작
sudo systemctl restart mysqld
```

---

## Redis 관리

```bash
# Redis 연결 테스트
sudo docker exec -it backend-redis-1 redis-cli ping
# 응답: PONG = 정상

# Redis CLI 진입
sudo docker exec -it backend-redis-1 redis-cli

# 캐시 전체 초기화 (주의: 모든 데이터 삭제)
sudo docker exec -it backend-redis-1 redis-cli FLUSHALL

# Redis 메모리 사용량 확인
sudo docker exec -it backend-redis-1 redis-cli INFO memory | grep used_memory_human

# Redis 컨테이너 재시작
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml restart redis
```

---

## Docker 관리

```bash
# 실행 중인 컨테이너
sudo docker ps

# 전체 컨테이너 (중지 포함)
sudo docker ps -a

# 이미지 목록
sudo docker images

# 사용하지 않는 리소스 전체 정리 (컨테이너, 이미지, 네트워크)
sudo docker system prune -f

# 컨테이너 직접 쉘 진입 (디버깅용)
sudo docker exec -it backend-app-1 sh

# 디스크 사용량 확인
sudo docker system df
```

---

## 서버 리소스 확인

```bash
# CPU / 메모리 사용량 실시간
top
# 종료: q

# 디스크 사용량
df -h

# 메모리 사용량
free -h

# 컨테이너별 CPU/메모리 사용량 (실시간)
sudo docker stats

# 컨테이너별 한번만 출력
sudo docker stats --no-stream
```

---

## 배포 문제 해결

```bash
# 앱이 안 뜰 때: 로그에서 에러 확인
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml logs --tail=100 app | grep -i "error\|exception\|failed"

# 포트 8080 사용 중인 프로세스 확인
sudo ss -tlnp | grep 8080

# MySQL 연결 확인
mysql -u root -p -e "SELECT 1;" 2>/dev/null && echo "MySQL OK" || echo "MySQL FAIL"

# Redis 연결 확인
sudo docker exec backend-redis-1 redis-cli ping 2>/dev/null && echo "Redis OK" || echo "Redis FAIL"

# 환경변수가 컨테이너에 잘 들어갔는지 확인
sudo docker inspect backend-app-1 | grep -A 30 '"Env"'

# 이미지 재빌드 (캐시 완전 무시)
cd ~/Backend && sudo docker compose -f docker-compose.prod.yml build --no-cache app
```

---

## GitHub Actions 자동 배포 확인

```bash
# 마지막 git pull 시점 확인
cd ~/Backend && git log --oneline -5

# 현재 브랜치
cd ~/Backend && git branch

# 배포 로그 확인 (nohup 배포 시)
tail -f ~/deploy.log
```
