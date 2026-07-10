---
description: >
  기능 e2e 테스트 작성·실행·수정 워크플로우. /qa <scope> <feature> 로 호출하면
  도커 컴포즈로 환경을 띄우고, 인수 테스트 기반 e2e 시나리오를 작성·실행한 뒤
  실패 시 프로덕션 코드를 수정하는 루프를 돈다.
arguments: [scope, feature]
---

`/qa` 는 `docs/<scope>/<feature>/` 의 인수 테스트들을 사용자 여정으로 변환해 검증하느 단계다

## 입력

- `$scope` — 기능이 속한 위치
  - api 모듈: `api/<context>` (예: `api/trading`)
  - 그 외 모듈: 모듈명 그대로 (예: `engine`, `collector`, `frontend`)
- `$feature` — 기능 이름 (kebab-case)

예시:
```
/qa api/trading place-order
/qa engine matching
```

인자가 부족하거나 형식이 맞지 않으면 사용자에게 호출 형태를 안내하고 종료한다.

## 사전 제약

- `docs/<scope>/<feature>/spec.md`, `plan.md` 가 존재해야 한다.
- `docs/<scope>/<feature>/index.md` 의 `단계` 가 `review` 여야 한다. 그 외 단계면 종료한다.
- `@<feature>` 태그가 붙은 인수 테스트(`.feature` 파일) 가 존재해야 한다.
- 모듈 정책상 인수 테스트를 두지 않는 모듈(`<module>/docs/testing.md` 의 `**인수 테스트**` 섹션이 `작성하지 않는다`) 이라면 `/qa` 를 적용하지 않는다 — 종료.

## 작업 범위 제한

- 이 기능 e2e 통과를 위한 **최소 수정만** 한다. 다른 기능 코드, 무관한 리팩터링 금지.
- 단위 테스트·인수 테스트는 **신규 작성하지 않는다**. 깨진 단위/인수 테스트가 있으면 사용자에게 보고만 한다.
- `plan.md`, `spec.md` 는 수정하지 않는다.
- 다른 기능의 e2e 를 깨뜨리지 않는다.


## 디렉터리 구조

```
e2e/
├── package.json
├── playwright.config.ts
└── <feature>.spec.ts
```

`/qa` 는 사실상 api 모듈 전용이라 (engine·collector 는 인수 테스트 미작성 정책) 모듈 하위 디렉토리 없이 평면으로 둔다. 나중에 e2e 가 많아지면 컨텍스트별로 묶는다.

## 흐름

### 1. 도커 컴포즈 기동

monitoring 스택은 e2e 에 무관하므로 빼고 필요한 서비스만 띄운다. `--wait` 로 모든 healthcheck 통과까지 대기한다.

```bash
docker compose up -d --wait backend engine collector frontend
```

`--wait` 가 timeout 으로 끊기면 `docker compose logs` 로 실패한 서비스 로그를 모아 보고하고 종료한다.

### 2. e2e 디렉토리 초기화 (없을 때만)

`e2e/package.json` 이 없으면 초기화한다.

```bash
mkdir -p e2e && cd e2e
npm init -y
npm install -D @playwright/test
npx playwright install chromium
```

`playwright.config.ts` 양식:

```ts
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './',
  testMatch: '**/*.spec.ts',
  fullyParallel: false,
  retries: 0,
  reporter: [['html'], ['list']],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
```

### 3. 시나리오 조립

이 기능의 인수 테스트들을 사용자 여정으로 묶는다.

**재료 모으기**

1. `<module>/src/test/resources/features/` 에서 `@<feature>` 태그된 시나리오를 전부 수집한다.
2. `plan.md` 의 시퀀스 플로우를 봐서 선행으로 필요한 흐름을 식별한다 (예: 주문 → 라운드 시작·로그인).
3. 선행 흐름에 대응하는 다른 기능의 인수 테스트(다른 `@<feature>`) 를 같은 모듈의 .feature 에서 찾아 수집한다.

**해피 패스 1개 조립**

선행 인수 테스트들 + 이 기능 정상 분기 인수 테스트를 시간순으로 한 여정으로 엮는다. test() 1개.

예 (`place-order`):
```
[선행] 회원가입 → [선행] 로그인 → [선행] 라운드 시작 → [이번] 매수 주문 → [이번] 주문 내역 확인
```

**핵심 실패 분기 1~2개**

이 기능 .feature 시나리오 중 결과 문장에 **UI 피드백이 명시된 것만** 고른다.
- 채택: "잔고 부족 모달이 뜬다", "주문 금액 부족 토스트가 표시된다", "잔고 입력 필드가 비활성화된다"
- 제외: "응답 코드는 400 이다", "에러 코드는 X 이다" (UI 피드백 아님)

1~2개로 추린다. 너무 많으면 도커·실행 비용 부담.

### 4. 시나리오 번역

API 수준 Gherkin 을 UI 액션으로 번역한다.

| API 수준 | UI 수준 |
|---------|---------|
| `BTC 잔고를 조회한다` | 포트폴리오 페이지에서 BTC 잔고가 화면에 표시 |
| `응답 상태코드는 200 이다` | 성공 메시지 또는 결과 UI 요소 확인 |
| `에러 코드는 X 이다` | "에러 메시지 텍스트" 가 표시 |
| `라운드 시작 요청을 보낸다` | 라운드 시작 버튼 클릭 → 시드머니 입력 → 시작 |

비즈니스 규칙(최소 금액, 잔고 부족 등) 은 그대로 유지. 배경(Given) 의 데이터 준비도 가능한 한 UI 액션으로 변환하고, 불가능하면 API 직접 호출로 남긴다.

### 5. spec.ts 스켈레톤 작성

`e2e/<feature>.spec.ts` 에 시나리오를 `test.skip()` 로 모두 등록한다.

원칙:
- 최상위 `test.describe` 로 감싸지 않는다 (리포트 prefix 방지).
- 해피 패스: serial 모드 + 공유 `page` 인스턴스로 단계 보장.
- 실패 분기: 각자 독립 test(). 해피 패스에 의존하지 않게 setup 별도.

### 6. 구현 (skip 하나씩 풀기)

`test.skip()` 을 위에서부터 하나씩 풀면서 구현 → 단일 실행 → 통과 확인.

```bash
cd e2e && npx playwright test <feature>.spec.ts
```

**작성 원칙**

- **테스트마다 새 사용자로 시작.** username/email 에 타임스탬프 붙여 유니크하게. 데이터는 사용자 단위로 묶이니 DB 안 비워도 격리됨.
- **숫자는 정확값 비교.** "주문 완료" 토스트로 끝내지 말고 잔고 = `초기잔고 - 체결가 × 수량 - 수수료` 로 검증.
- **`getByText` 중복 매칭 주의.** 화면에 같은 글자가 2개 이상이면 에러. `{ exact: true }` 또는 `.first()`.
- **화면 밖 요소는 `scrollIntoViewIfNeeded()` 후 클릭.** 안 되면 `page.evaluate()` 로 JS 클릭.
- **모달 버튼 클릭 안 먹히면 `page.evaluate()` 로 JS 직접 클릭.** Radix UI 같은 라이브러리에서 종종 발생.

### 7. 전체 실행 + 수정 루프

전체 실행:

```bash
cd e2e && npx playwright test <feature>.spec.ts
```

**시나리오마다 최대 3회 재시도.** 한 시나리오가 3회차에도 깨지면 그 시나리오만 포기하고 증거를 모은다. 다른 시나리오는 계속 진행한다.

각 회차마다:

1. **실패 증거 수집** (모두 모은다):
   - 에러 메시지 (예상값 vs 실제값을 함께 기록)
   - 스크린샷 (Playwright 가 자동 저장한 것)
   - 브라우저 콘솔 로그
   - 네트워크 로그 (4xx / 5xx 응답 여부 명시)
   - Playwright trace 파일 경로
2. **디버깅**: 스택 트레이스를 따라 프로덕션 코드를 읽고 버그를 식별한다. spec.md / plan.md 재참조는 코드에 원인이 정말 없을 때만.
3. **수정**: 의심되는 프로덕션 코드를 고친다. e2e 자체가 명백히 이상한 경우에만 테스트를 건드린다.
4. **재실행**: 같은 명령으로 다시 돌린다.
5. **커밋 (통과 시)**: `docs/git-convention.md` 컨벤션에 맞게 커밋한다.

### 8. 결과 처리

- **모두 통과**: `<feature>/index.md` 의 `단계` → `qa`, `QA 회차` → `N/3` (N = 마지막 시나리오가 통과한 재시도 회차).
- **일부 실패**: `단계` 미갱신 (review 유지), `QA 회차` → `3/3`. 잔존 실패 시나리오와 증거를 사용자에게 보고한다.

### 9. 도커 정리

데이터 볼륨만 지운다. `frontend-node-modules` 는 보존해서 다음 호출 시 `npm install` 을 다시 돌리지 않게 한다.

```bash
docker compose down
docker volume rm -f trypto_mysql-data trypto_redis-data trypto_rabbitmq-data trypto_influxdb-data trypto_engine-wal
```

다음 호출은 깨끗한 DB 상태에서 시작한다.

### 10. 보고

```
QA 완료: docs/<scope>/<feature>/

e2e 파일: e2e/<feature>.spec.ts
시나리오: 해피 패스 1 + 실패 분기 N
실행 결과: 통과 / 실패 (통과 n / 전체 m)
재시도: 마지막 통과 회차 / 미통과 시나리오는 3/3
커밋: <SHA 단축 목록 또는 "없음">
단계 갱신: qa / 미갱신 (사유)
잔존 실패 (있는 경우):
- <시나리오 제목>: <한 줄 원인>
  - 증거: <스크린샷·trace 경로>
```
