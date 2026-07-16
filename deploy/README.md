# 프로덕션 배포 (Oracle Cloud)

배포는 두 가지로 나뉜다. **최초 1회 부트스트랩**과 **반복 배포**를 혼동하지 않는다.

| 구분 | 무엇 | 언제 |
|------|------|------|
| 부트스트랩 | 인플럭스 시드, DNS, 인증서, DB 생성, 최초 스키마 | 서버를 처음 세울 때 딱 한 번 |
| 반복 배포 | `bash deploy/deploy.sh` | 코드가 바뀔 때마다 |

인프라 생성(terraform)은 [terraform/prod/README.md](../terraform/prod/README.md) 를 따른다.

---

## 반복 배포

```
bash deploy/deploy.sh                # 빌드/푸시 + 서버 반영
bash deploy/deploy.sh --build-only   # 서버가 아직 없을 때 이미지만 준비
```

모듈별 소스 해시로 태그를 만들므로, 코드가 같으면 빌드·푸시·재기동이 전부 스킵된다.
`VITE_` 값(소셜 로그인 주소 등)을 바꾸면 frontend 태그가 바뀌어 자동으로 다시 빌드된다.
데이터 볼륨은 절대 건드리지 않는다.

---

## 최초 1회 부트스트랩

전제: terraform apply 완료, `app_public_ip` 확보. 아래에서 `<IP>` 는 그 값이다.

### 1. DNS

Cloudflare → trypto.dev → DNS → A 레코드 생성: 이름 `@`, 값 `<IP>`, **프록시 꺼짐(회색 구름)**.

### 2. 시계열 DB 시드 (D 드라이브 → 서버 볼륨)

컨테이너를 처음 띄우기 **전에** 한다. 볼륨이 비어 있지 않으면 인플럭스 초기 셋업이 자동으로 생략되고, 시드에 들어 있는 조직/버킷/토큰/집계 태스크가 그대로 산다.

```bash
# PC (레포 루트)
tar -C /d/trypto-influx-data -czf influx-seed.tar.gz .
scp -i ~/.ssh/trypto_oracle influx-seed.tar.gz ubuntu@<IP>:/opt/trypto/

# 서버
docker volume create trypto_influxdb-data
docker run --rm -v trypto_influxdb-data:/target -v /opt/trypto:/src alpine \
  sh -c "tar -xzf /src/influx-seed.tar.gz -C /target && chown -R 1000:1000 /target"
rm /opt/trypto/influx-seed.tar.gz
```

### 3. 데이터베이스 생성 (관리형 MySQL)

기존 MySQL 컨테이너와 콜레이션을 맞춰서 만든다.

```bash
# 서버 (비밀번호는 .env 의 MYSQL_PASSWORD)
mysql -h mysql.db.trypto.oraclevcn.com -u trypto -p \
  -e "CREATE DATABASE trypto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 4. 첫 배포 + 최초 스키마

```bash
# PC
bash deploy/deploy.sh
```

테이블이 하나도 없는 최초 1회만: 서버의 `/opt/trypto/.env` 에서 `HIBERNATE_DDL_AUTO=create` 로 바꿔
`docker compose -f docker-compose.prod.yml up -d backend` 로 한 번 띄우고, 헬시 확인 후 **곧바로 `none` 으로 되돌려** 다시 `up -d backend` 한다.

### 5. 인증서 + 호스트 nginx

DNS 전파 후(1 단계로부터 수 분) 서버에서:

```bash
sudo certbot certonly --nginx -d trypto.dev
sudo cp /opt/trypto/deploy/nginx-trypto.conf /etc/nginx/conf.d/trypto.conf   # deploy.sh 가 올려둔 파일
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

갱신은 certbot 이 systemd 타이머로 자동 수행한다. `sudo certbot renew --dry-run` 으로 확인한다.

### 6. 인플럭스 토큰 교체 (보안 위생)

시드에 들어 있는 토큰은 개발용 약한 값이므로 부팅 후 교체한다.

```bash
# 서버
cd /opt/trypto
docker compose -f docker-compose.prod.yml exec influxdb \
  influx auth create --org trypto --all-access
# 출력된 토큰을 .env 의 INFLUXDB_TOKEN 에 반영 후
docker compose -f docker-compose.prod.yml up -d backend collector
```

### 7. 소셜 로그인 콘솔 등록 (아직 안 했다면)

- 카카오: Web 플랫폼 도메인 `https://trypto.dev` + Redirect URI `https://trypto.dev/auth/kakao/callback` 추가
- 구글: 승인된 리디렉션 URI `https://trypto.dev/auth/google/callback` + JS 원본 `https://trypto.dev` 추가, 동의 화면 게시 상태 "프로덕션"

### 8. 확인

- `https://trypto.dev` 접속 → 시세 표시·소셜 로그인·주문까지 눈으로 확인
- 그라파나: `ssh -i ~/.ssh/trypto_oracle -L 3000:localhost:3000 ubuntu@<IP>` 후 `http://localhost:3000`

---

## 트러블슈팅

| 증상 | 확인 |
|------|------|
| 접속 자체가 안 됨 | 서버 iptables (`sudo iptables -L INPUT -n \| head`) — cloud-init 이 80/443 을 열었는지 |
| 502 | 프론트 컨테이너 상태 (`docker compose ps`), 8081 바인딩 |
| 로그인 무한 루프/쿠키 문제 | https 로 접속했는지 (SESSION_COOKIE_SECURE=true 는 http 에서 동작 안 함) |
| buildx 가 --push 를 거부 | `docker buildx create --use` 로 빌더 생성 후 재시도 |
| compose pull 실패 | 서버에서 `docker login` (프라이빗 이미지가 아니면 불필요) |
