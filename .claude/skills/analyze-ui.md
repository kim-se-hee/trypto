---
description: >
  프론트엔드 UI 분석으로 백엔드 API 요구사항 도출. ui-analyst 에이전트에 위임하여
  프론트엔드 페이지의 TSX 컴포넌트, mock 데이터, Playwright 스크린샷을 분석하고
  백엔드가 제공해야 할 API 목록을 역추적한다. api-catalog.json과 대조하여 누락된 API를 찾는다.
  TRIGGER: 사용자가 UI 분석을 요청하거나 /analyze-ui를 입력할 때.
---

# UI 분석

ui-analyst 에이전트에 위임하여 프론트엔드 페이지를 분석하고 백엔드 API 요구사항을 도출한다.

## 입력

`$ARGUMENTS` = 페이지 경로 (예: `/wallet`, `/portfolio`)

---

## 워크플로우

### 1. 프론트 dev 서버 확인

`localhost:5173`에 프론트 dev 서버가 실행 중인지 확인한다.

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:5173/ 2>/dev/null || echo "not running"
```

- 실행 중이 아니면 안내 메시지를 출력한다: "프론트 dev 서버가 실행되지 않고 있습니다. `cd src/main/frontend && npm run dev`로 시작해주세요. 코드 분석만으로 진행합니다."

### 2. 페이지 매핑

`$ARGUMENTS` 경로를 프론트엔드 페이지 파일에 매핑한다:

| 경로 | 페이지 파일 |
|------|------------|
| `/wallet` | `WalletPage.tsx` |
| `/market` | `MarketPage.tsx` |
| `/portfolio` | `PortfolioPage.tsx` |
| `/ranking` | `RankingPage.tsx` |
| `/regret` | `RegretPage.tsx` |
| `/round/create` | `RoundCreatePage.tsx` |
| `/login` | `LoginPage.tsx` |
| `/mypage` | `MyPage.tsx` |

- 매핑되지 않으면 `src/main/frontend/src/pages/` 디렉토리를 검색하여 가장 가까운 파일을 찾는다

### 3. ui-analyst 에이전트 호출

ui-analyst 에이전트에 아래 정보를 전달하여 위임한다:

- **페이지 경로**: `$ARGUMENTS`
- **페이지 파일**: 매핑된 TSX 파일
- **dev 서버 상태**: 실행 여부

### 4. 결과 출력

에이전트의 분석 결과를 화면에 출력한다. 파일은 생성하지 않는다.

---

## 사용법

```
/analyze-ui /wallet
/analyze-ui /portfolio
/analyze-ui /market
```
