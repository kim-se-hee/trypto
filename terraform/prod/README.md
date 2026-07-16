# 프로덕션 인프라 (Oracle Cloud, Always Free)

trypto 서비스 운영 인프라를 정의한다. 부하테스트 스택(`terraform/`)과는 상태 파일이 분리된 별개 스택이다.

## 구성 요소

| 리소스 | 내용 | 비용 |
|--------|------|------|
| VCN + 서브넷 2개 | 퍼블릭(VM) / 프라이빗(DB) 분리 | 무료 |
| A1 인스턴스 | 2 OCPU / 12GB / 부트 100GB, Ubuntu 24.04 ARM | 무료 (Always Free) |
| 고정 공인 IP | 인스턴스 재생성에도 유지 | 무료 |
| 관리형 MySQL | HeatWave Always Free, 50GB, 일일 자동 백업 | 무료 (계정당 1개) |

모든 리소스는 홈 리전(오사카)에만 생성할 수 있다.

## 사전 준비

1. **API 키** — 콘솔 우상단 프로필 → 내 프로파일 → API 키 → "API 키 추가" → 키 쌍 생성 후 개인 키를 `C:/Users/saree98/.oci/` 에 저장한다. 생성 직후 표시되는 "구성 파일 미리보기"에서 `tenancy` `user` `fingerprint` 값을 옮겨 적는다.
2. **SSH 키** — `ssh-keygen -t ed25519 -f C:/Users/saree98/.ssh/trypto_oracle`
3. **변수 파일** — `terraform.tfvars.example` 을 `terraform.tfvars` 로 복사해 값을 채운다.

## 실행

```
terraform -chdir=terraform/prod init
terraform -chdir=terraform/prod plan
terraform -chdir=terraform/prod apply
```

### A1 용량 부족이 발생하는 경우

`Out of host capacity` 오류는 계정 문제가 아니라 해당 가용 영역의 무료 ARM 재고 소진이다. 시간을 두고 apply 를 재시도한다. 수일간 계속되면 종량제(PAYG) 전환으로 우선순위를 높이는 방법이 있다 — Always Free 한도 내 사용은 전환 후에도 무료다.

## apply 이후 절차

1. 출력된 `app_public_ip` 를 Cloudflare 의 `trypto.dev` A 레코드로 등록한다 (프록시 꺼짐).
2. VM 에 접속해 데이터베이스를 만든다 (기존 컨테이너 MySQL 과 콜레이션을 맞춘다):
   ```
   mysql -h mysql.db.trypto.oraclevcn.com -u trypto -p
   CREATE DATABASE trypto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. 인증서 발급, `.env` 업로드, compose 기동은 배포 스크립트 문서를 따른다.

## 주의 사항

- `MySQL.Free` shape 는 Always Free 전용이며 스토리지 50GB 로 고정된다. 서버 시간대는 구성 리소스(`trypto-kst`)가 `+09:00` 으로 지정한다 — 기존 MySQL 컨테이너가 강제하던 값과 동일해야 시간 데이터가 일관된다.
- 인스턴스 shape 를 2 OCPU / 12GB 초과로 바꾸면 Always Free 한도를 벗어난다 (2026-06 축소 이후 기준).
- 체험 크레딧 기간(가입 후 30일) 중에도 이 구성은 전부 무료 한도 내이므로, 크레딧 만료 시 회수되는 리소스가 없다.
