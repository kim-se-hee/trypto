---
description: >
  로컬 도커 스택에 부하를 걸어 병목을 자동 진단하고, 아키텍처 변경 미만의 문제는 자동으로 고쳐
  재측정으로 검증하는 자율 루프. 측정과 판정은 메인이, 해석과 진단은 performance-diagnostician 이,
  반영은 performance-improver 가 맡는다. 지표가 모자라면 계측을 추가해서라도 위치를 찾고,
  아키텍처를 바꿔야 하는 병목은 손대지 않고 제안 리포트만 남긴다.
  /performance-tune <scenario> 로 호출. AWS 풀스케일 검증은 /performance-test.
  TRIGGER: 사용자가 로컬 성능 자동 개선을 요청하거나 /performance-tune 를 입력할 때.
arguments: [scenario]
---

`/performance-tune` 는 로컬 자율 성능 개선 루프다. 부하를 걸어 측정하고, 병목을 진단하고, 고치고, 다시 측정해 효과를 증명한다. 이 사이클을 목표를 만족하거나 더 손댈 게 없을 때까지 반복한다.

## 역할 분담

| 역할 | 하는 일 |
|---|---|
| 메인 (이 스킬) | 측정(k6 실행), 수치 비교로 유지/되돌림 판정, 사이클 진행과 종료, 리포트 작성 |
| performance-diagnostician | 측정 결과 해석과 병목 진단. 병목마다 AUTO(자동 수정) / INSTRUMENT(계측 추가) / ESCALATE(아키텍처) 로 분류해 반환 |
| performance-improver | 같은 모듈의 진단 결과 묶음을 받아 건별로 반영·커밋. 한 호출 = 한 모듈 묶음 |

분류 기준과 작업 규칙은 각 서브에이전트 정의에 있다. 메인은 직접 해석하거나 고치지 않고, 진단 결과의 `분류` 라벨로 처리 경로만 가른다.

**전파 메모**: 서브에이전트 보고에서 코드 변경과 무관하게 반복 비용을 만드는 환경 사실(원래부터 실패하던 테스트, 도구 출력 특성 등)이 나오면 `$REPORT` 의 "전파 메모" 섹션에 한 줄로 적는다. 이후 모든 진단·반영 호출 프롬프트 끝에 이 섹션을 그대로 붙인다. 같은 사실을 서브에이전트마다 재발견(테스트 재실행 등)하지 않게 하는 장치다.

## 입력

`$scenario` = `loadtest/k6/scenarios/` 안의 파일명 (예: `place_order.js`). 필수.
없거나 파일이 존재하지 않으면 시나리오 목록을 보여주고 종료한다.

부하 모양과 합격선은 시나리오 파일에 박힌 그대로 쓴다. 부하 환경변수(`RAMP_DUR` 등)를 주입하지 않고, 합격선은 시나리오의 k6 threshold 다.

## 사전 점검

하나라도 실패하면 한 줄로 알리고 중단한다.

| 항목 | 검증 |
|---|---|
| 시나리오 존재 | `test -f loadtest/k6/scenarios/$scenario` |
| Docker 데몬 | `docker info` 성공 |
| reset 스크립트 | `test -f loadtest/reset.sh` |
| 깨끗한 작업 트리 | `git status --porcelain` 출력 없음 (되돌리기가 `reset --hard` 라 커밋 안 된 변경은 날아간다) |

## 고정값

```
REPORT       = loadtest/tuning/<STEM>-<시작시각>.md   # 진단·개선 내역을 사이클마다 누적 기록
MAX_CYCLES   = 5    # 진단 → 변경 → 재측정 사이클 상한
OVERHEAD_MAX = 3%   # 계측만 반영한 사이클이 p99 를 이보다 나쁘게 하면 되돌림
```

## 흐름

측정 → 진단 → 변경 → 재측정 사이클이다. 한 사이클은 진단 한 번이 낸 AUTO·INSTRUMENT 전부를 반영한다. 
성능이 좋아지면 마지막 커밋이 새 기준이 되고, 나빠지면 기준 커밋으로 되돌린다. 

### 1. 준비 + 측정 (베이스라인)

현재 체크아웃된 브랜치 위에서 그대로 진행한다. 브랜치를 새로 만들지 않는다.
지금 HEAD 를 **시작 커밋**으로 기록해 둔다 — 최종 측정이 불합격이면 루프가 만든 커밋을 전부 버리고 여기로 돌아간다.
`reset.sh` 로 스택을 띄워 healthy 를 기다린 뒤 k6 를 끝까지 돌린다.
로컬 루프는 Hub 이미지가 아니라 **작업트리 코드**를 측정해야 하므로 `RESET_BUILD_LOCAL=1` 로 reset.sh 를 호출한다 (cold path 가 pull 대신 `docker compose build`). AWS `performance-test` 는 이 플래그를 안 줘서 pull 그대로다.

```bash
NET=trypto-net
STEM=${scenario%.js}
mkdir -p loadtest/tuning
RESET_BUILD_LOCAL=1 bash loadtest/reset.sh "$scenario"

run_k6() {
  local label=$1 log="loadtest/tuning/k6-$STEM-$label.log"
  # Git Bash(MSYS)는 /scripts... 컨테이너 경로를 호스트 경로로 변환한다 → MSYS_NO_PATHCONV=1 로 끈다.
  # 마운트 소스는 Git Bash 에선 Windows 경로(pwd -W), Linux 에선 그냥 $PWD.
  local k6dir="$PWD/loadtest/k6"
  case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) k6dir="$(pwd -W)/loadtest/k6";; esac
  case "$scenario" in
    ticker_websocket.js)
      SCENARIO_ENV=(-e API_HOST=backend:8080 -e COLLECTOR_HOST=collector:8081 -e RUN_RAMP_SETUP=true) ;;
    match_pending*.js)
      SCENARIO_ENV=(-e API_TARGET=http://backend:8080 -e COLLECTOR_TARGET=http://collector:8081) ;;
    *)
      SCENARIO_ENV=(-e API_TARGET=http://backend:8080) ;;
  esac
  MSYS_NO_PATHCONV=1 docker run --rm --network "$NET" \
    -v "$k6dir:/scripts:ro" \
    "${SCENARIO_ENV[@]}" \
    -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
    -e K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true \
    grafana/k6:latest run -o experimental-prometheus-rw "/scripts/scenarios/$scenario" \
    2>&1 | tee "$log"
  return "${PIPESTATUS[0]}"   # 0 = threshold 통과, 99 = 불합격
}

run_k6 baseline
```

종료 코드가 곧 판정이다: 0 합격, 99 불합격. 수치 확인은 로그 꼬리로만 한다 (`tail -n 50` + threshold·커스텀 지표 grep). 로그 대부분이 초당 진행 출력이라 전체를 Read 하지 않는다. 전체 로그는 진단가에게 경로로만 넘긴다. 이 측정이 출발점이다. 되돌림이 돌아갈 **기준 커밋**(현재 HEAD)과 그 측정 수치(p99·실패율·throughput·측정 시간창)를 기록해 둔다. 처음엔 기준 커밋이 곧 직전 측정 지점이고, 이후 진단·비교는 직전 측정을, 되돌림은 기준 커밋을 본다.

- 합격: 이미 목표를 만족하므로 고칠 게 없다. 5번(최종 보고)으로.
- 불합격: 2번(진단)으로.

### 2. 진단

`performance-diagnostician` 을 호출하면서 프롬프트에 전달한다:

- `scenario`: 시나리오 파일명
- `k6_log`: 직전 측정의 k6 로그 경로
- `summary`: 직전 측정의 p99 / 실패율 / throughput 과 threshold 통과 여부
- `window`: 직전 측정의 시작~종료 시각
- `excluded`: 이미 고쳐 봤지만 효과가 없어 되돌린 병목들의 목록. 항목마다 `위치`(어디)와 `내용`(무슨 문제였는지)을 함께 적는다. 같은 파일 같은 줄이라도 문제가 다르면 다른 병목이므로, `위치`와 `내용`이 둘 다 같을 때만 같은 병목으로 본다. 첫 사이클엔 비어 있고, 되돌릴 때마다 한 줄씩 쌓인다.

진단가는 진단 결과 목록을 영향 큰 순서로 돌려준다 (건마다 위치 / 증거 / 분류 / 처리 네 칸 고정). 목록을 `$REPORT` 에 기록하고 가른다:

- ESCALATE 건은 코드를 건드리지 않는다. 바로 `$REPORT` 의 "아키텍처 제안" 에 적는다: 메트릭 증거, 왜 자동 수정이 안 되는지, 개선안 옵션과 트레이드오프. 
- ESCALATE 가 하나라도 나오면 루프를 즉시 끝낸다. 자동 수정으로는 목표를 만족할 수 없다는 판정이므로, 같은 진단의 남은 AUTO·INSTRUMENT 도 반영하지 않고 후보로만 `$REPORT` 에 적는다. `git reset --hard <시작 커밋>` 으로 루프 커밋을 전부 버리고 5번(최종 보고)으로 간다.
- 진단가가 그래도 `excluded` 와 위치·내용이 똑같은 병목을 올렸으면 메인이 반영 목록에서 뺀다 (안전망).
- 남은 AUTO·INSTRUMENT 전부가 이번 사이클의 **반영 목록**이다.

반영 목록이 비어 있으면 5번으로, 아니면 3번으로.

### 3. 변경

반영 목록을 모듈별로 묶는다 (`api/<context>` 는 전부 `api` 한 묶음). 묶음마다 `performance-improver` 를 한 번 호출한다 (한 호출 = 한 모듈의 finding 묶음). 프롬프트에 `scope`(모듈), `scenario`, `findings`(진단 결과 본문 + 종류 `AUTO`/`INSTRUMENT`, 영향 큰 순서), 그리고 `$REPORT` 의 전파 메모를 전달하고, 보고를 처리한 뒤 다음 묶음으로 넘어간다. improver 가 건별로 커밋하므로 묶어도 건 단위 되돌림은 유지된다.

- `적용 완료`: 다음 묶음으로.
- `막힘(...)`: 예상 밖 실패다(컴파일·테스트·적용). 무엇이 왜 막혔는지를 `$REPORT` 에 적고 사이클을 끝낸다. 막힌 건은 improver 가 커밋 없이 원복했고, 이번 사이클에서 그 전에 적용한 커밋도 재측정으로 검증된 게 아니므로 기준 커밋으로 되돌려 작업 트리를 비운 뒤 5번(최종 보고)으로 간다.

목록을 다 처리한 뒤 모듈의 이미지를 재빌드하고 4번으로. 매핑: `api → backend`, `collector → collector`, `engine → engine`.
  ```bash
  docker compose build <service...>
  ```

### 4. 재측정 + 판정

스택을 다시 띄우고(`RESET_BUILD_LOCAL=1 bash loadtest/reset.sh "$scenario"`) k6 를 한 번 더 돌린다(`run_k6 cycle-<n>`, `<n>` 은 사이클 번호). 
그 수치를 직전에 유지된 측정과 비교해 이번 사이클의 변경을 유지할지 되돌릴지 정한다.

- 성능이 좋아졌으면 유지한다: p99 가 내려가고, 실패율이 늘지 않고, k6 종료 코드가 나빠지지 않았을 때.
- 이번 사이클 적용 사항이 계측(INSTRUMENT)만으로 이뤄졌으면 성능이 하락하더라도 사이클을 종료하지 않는다.
- 둘 다 아니면 기준 커밋으로 되돌아 간다.

유지한 뒤 기준 커밋을 어떻게 둘지가 갈린다.

- 성능이 좋아져서 유지한 경우: 기준 커밋을 이번 HEAD 로 옮긴다
- 계측만이라 유지한 경우: 성능이 오른 게 아니므로 기준 커밋은 그대로 두고 새로운 커밋만 그 위에 쌓는다.

되돌림이면 `git reset --hard <기준 커밋>` 으로 기준 커밋으로 돌아간 뒤 모듈을 재빌드하고, 사유(효과 없음 / 회귀 / 부담 과다)와 함께 `$REPORT` 에 기록한다. 
코드가 기준 커밋으로 돌아갔으니 직전 측정도 기준 커밋의 측정으로 되돌린다. 
그리고 되돌린 병목들을 **제외 목록**에 추가한다 (각각 `위치` + `내용`). 
되돌리면 코드와 직전 측정이 기준으로 돌아가, 다음 진단이 이미 진단했던 병목 후보를 또 올릴 수 있다. 제외 목록으로 이미 진단한 같은 병목은 넘어가고 다음 사안을 진단할 수 있다.

```bash
git reset --hard <기준 커밋>
docker compose build <service...>
```

판정 후 행선지:

- 재측정이 threshold 를 통과함(k6 종료 코드 0): 목표 달성. 5번으로.
- `MAX_CYCLES` 도달: 5번으로.
- 그 외: 2번(재진단)으로. 병목을 풀면 다음 병목이 드러난다.

### 5. 최종 보고

마지막에 유지된 측정이 threshold **불합격**(k6 종료 코드 ≠ 0)이면 코드를 남기지 않는다: `git reset --hard <시작 커밋>` 으로 루프가 만든 커밋을 전부 버린다. 부분 개선이 있었어도 마찬가지다 — 무엇을 바꿔 얼마나 좋아졌는지는 `$REPORT`(untracked 라 reset 에도 남는다)가 보존하므로, 합격 가능한 구조 변경 후 거기서 다시 시작하면 된다. 이 경우 아래 계측 정리는 건너뛰고 바로 보고로 간다.

threshold **합격**일 때만 루프 도중 추가한 계측 지표를 정리한다.
- 쓸모 있던 지표(수정할 곳을 찾았거나 아키텍처 제안의 근거가 된 것): 모니터링을 위해 코드에 남기고 `$REPORT` 의 "추가한 지표" 에 적는다.
- 끝까지 아무것도 못 짚은 지표: 떼어낸다. 해당 지표를 추가한 커밋 위에 다른 커밋이 쌓였을 수 있어 `reset` 으로 되감으면 멀쩡한 커밋까지 날아가므로, `git revert --no-edit <계측 커밋>` 으로 그 커밋만 취소한다.

`$REPORT` 를 마무리한 뒤 아래 형식으로 보고하고 스택을 정리한다. 볼륨은 남긴다 (다음 reset.sh 가 알아서 비운다).

```bash
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml down --remove-orphans
```

내용 없는 섹션은 통째로 뺀다. 추가한 지표가 없으면 그 섹션을 적지 않고, 되돌린 시도와 아키텍처 제안도 마찬가지다.

```
완료: /performance-tune <scenario>  (브랜치: <현재 브랜치>)

[측정: 베이스라인 → 최종]   # 고친 게 없으면 베이스라인이 곧 최종
p99:        <before>ms → <after>ms     목표 <threshold>ms  (통과/미달)
실패율:      <before>% → <after>%       목표 <threshold>   (통과/미달)
throughput: <수치> req/s (dropped/interrupted 0)

[원복]   # 최종 불합격 또는 ESCALATE 로 커밋을 버렸을 때만
- 시작 커밋 <hash> 로 복귀. 시도한 변경과 효과는 <REPORT 경로> 참조

[아키텍처 제안]
- <제목>: <한 줄>

[막힘]
- <위치> 막힘(<단계>): <핵심 한 줄>

[자동 개선]
- <파일:라인> <한 줄> (p99 <delta>)

[추가한 지표]
- <metric 이름> <무엇을 보려고>
```
