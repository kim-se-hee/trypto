---
name: loadtest
description: Run a k6 load test end-to-end. Boots infra if down, waits for healthy state, resets SUT volumes for a clean run, launches k6 in the background on loadgen, then prints dashboard URLs and exits. Use when the user asks to start/run a load test (e.g. "/loadtest match_pending", "부하테스트 돌려줘").
---

# /loadtest

부하테스트 한 방 실행. EC2(loadgen)에서 k6가 백그라운드로 돌고, 이 스킬은 URL만 알려주고 즉시 종료합니다. 정리는 사용자가 `/loadtest-down`으로 직접 합니다.

## 입력

시나리오 이름 하나만 받습니다 (VU/duration 등은 시나리오 파일 안 `options`에 정의됨 — 튜닝하려면 시나리오 파일을 직접 수정):

- `match_pending` → `loadtest/k6/scenarios/match_pending.js`
- `place_order` → `loadtest/k6/scenarios/place_order.js`
- 또는 전체 경로: `loadtest/k6/scenarios/foo.js`

시나리오를 안 줬으면 `match_pending` 기본값.

## 절차

순서대로 실행. 각 단계 실패하면 즉시 중단하고 사용자에게 알립니다.

스킬 안에서 1·4 단계는 출력을 파이프로 이어 한 번에 처리해도 됨:
`bash scripts/build-images.sh | bash scripts/sync-to-ec2.sh`

1. **이미지 빌드/푸시** — `bash scripts/build-images.sh`
   - `trypto-api/`, `trypto-collector/` 각각 HEAD 해시(+dirty 접미사)로 태그 계산
   - Docker Hub 에 이미 있으면 빌드 스킵 (수 초), 없으면 `docker buildx --platform linux/amd64 --push`
   - stdout 으로 `API_TAG=...`, `COLLECTOR_TAG=...` 출력

2. **인프라 보장** — `bash scripts/ensure-infra.sh`
   - 인스턴스 다 떠있고 SSH 22 열려있으면 skip
   - 아니면 `terraform apply` (수 분 소요 가능)

3. **준비 대기** — `bash scripts/wait-ready.sh`
   - 양쪽 인스턴스 `/home/ubuntu/READY` 파일 생기길 대기
   - SUT의 `localhost:8080/actuator/health`가 `UP` 응답할 때까지 대기
   - 최대 20분 타임아웃

4. **워크스페이스 동기화** — `API_TAG=... COLLECTOR_TAG=... bash scripts/sync-to-ec2.sh`
   - outer 레포가 추적하는 파일(compose/SQL/infra config/loadtest) 을 SUT 에 rsync
   - `/home/ubuntu/trypto/.env` 에 이번 회차 이미지 태그 기록

5. **SUT 초기화** — `bash scripts/reset-sut.sh`
   - SUT에서 `docker compose down -v && pull && up -d` (DB/Redis/큐 리셋 + .env 의 새 이미지 태그 당겨오기)
   - 다시 healthy 될 때까지 대기 (스크립트 내부에서 자체 폴링)

6. **k6 실행** — `bash scripts/launch-k6.sh <scenario-path>`
   - 시나리오 경로는 `loadtest/k6/scenarios/<name>.js` 형태로 변환
   - loadgen에 `nohup`으로 던지고 즉시 SSH 종료
   - 이미 k6가 돌고 있으면 에러 — 사용자에게 `/loadtest-down` 안내

7. **URL 출력** — terraform output에서 EIP를 읽어 사용자에게 표시:
   ```
   k6 dashboard:  http://<loadgen-eip>:5665
   Grafana:       http://<sut-eip>:3000
   RabbitMQ UI:   http://<sut-eip>:15672
   k6 log tail:   ssh -i ~/.ssh/trypto-key-pair.pem ubuntu@<loadgen-eip> 'tail -f /home/ubuntu/k6.log'

   끝나면 /loadtest-down 으로 인스턴스 정리하세요.
   ```

## 작업 디렉터리

모든 스크립트는 프로젝트 루트(`C:/Users/saree98/intellij-workspace/trypto`)에서 실행하세요.

## 주의

- **이 스킬은 k6 완료를 기다리지 않습니다.** 백그라운드로 던지고 즉시 종료.
- 사용자가 브라우저 대시보드로 실시간 모니터링합니다.
- 인프라/인스턴스 정리는 **반드시 `/loadtest-down`**으로. 자동 teardown 없음.