# 개요

거래소별 포트폴리오의 보유 코인·잔고·평균매수가·현재가를 조회하는 REST API다.

# 목적

- 사용자가 특정 거래소의 투자 현황을 한눈에 파악할 수 있도록 데이터를 제공한다
- 보유 코인별 보유수량, 평균매수가, 현재가를 반환한다

# 입력 정보

- `userId` (Long): 사용자 ID
- `walletId` (Long): 조회할 지갑 ID (거래소별 지갑)

# 검증

| 항목 | 규칙 | 실패 시 에러 |
|------|------|-------------|
| 지갑 존재 | 해당 walletId의 지갑이 존재해야 한다 | `WALLET_NOT_FOUND` |
| 지갑 소유권 | GetWalletOwnerIdUseCase로 소유자 ID를 조회하여 userId와 비교 | `WALLET_NOT_OWNED` |

# 처리 로직

서버는 보유 코인의 정적 데이터와 현재가를 함께 조회하여 반환한다.

1. GetWalletOwnerIdUseCase로 지갑 소유권을 검증한다
2. 지갑을 조회한다
3. 거래소 정보를 조회하여 기축통화를 확인한다
4. 지갑의 기축통화(KRW 또는 USDT) 잔고를 조회한다
5. FindEvaluatedHoldingsUseCase로 보유 코인 목록을 조회한다 (현재가 포함)
6. 각 코인의 심볼·이름을 조회한다
7. 결과를 조합하여 반환한다

## 정렬

- 서버는 정렬하지 않는다
- 클라이언트에서 코인명, 보유수량, 평균매수가, 현재가, 평가금액, 평가손익, 수익률 기준 정렬을 지원한다 (프론트엔드 정렬)

# 크로스 도메인 의존

| From → To | UseCase | 용도 |
|-----------|---------|------|
| Portfolio → Wallet | GetWalletOwnerIdUseCase | 지갑 소유자 확인 |
| Portfolio → Wallet | FindWalletUseCase | 지갑 조회 |
| Portfolio → MarketData | FindExchangeDetailUseCase | 거래소 기축통화(baseCurrencyCoinId) 조회 |
| Portfolio → Wallet | GetAvailableBalanceUseCase | 기축통화 잔고 조회 |
| Portfolio → Trading | FindEvaluatedHoldingsUseCase | 평가된 보유 자산 조회 (현재가 포함) |
| Portfolio → MarketData | FindCoinInfoUseCase | 코인 심볼·이름 조회 |

# API 명세

## 참고사항

- 거래소 탭(업비트/빗썸/바이낸스) 전환은 클라이언트가 walletId를 바꿔서 호출한다
- 정렬은 클라이언트에서 처리한다 (데이터량이 적으므로)
- 현재가는 서버에서 FindEvaluatedHoldingsUseCase를 통해 조회하여 응답에 포함한다

## REST API

`GET /api/users/{userId}/wallets/{walletId}/portfolio`

### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 사용자 ID |
| walletId | Long | O | 조회할 지갑 ID |

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "포트폴리오를 조회했습니다.",
  "data": {
    "exchangeId": 1,
    "baseCurrencyBalance": 2450000,
    "baseCurrencySymbol": "KRW",
    "holdings": [
      {
        "coinId": 1,
        "coinSymbol": "BTC",
        "coinName": "비트코인",
        "quantity": 0.052341,
        "avgBuyPrice": 132500000,
        "currentPrice": 135000000
      },
      {
        "coinId": 2,
        "coinSymbol": "ETH",
        "coinName": "이더리움",
        "quantity": 1.245,
        "avgBuyPrice": 5120000,
        "currentPrice": 5350000
      }
    ]
  }
}
```

### 필드 설명

**data**

| 필드 | 타입 | 설명 |
|------|------|------|
| exchangeId | Long | 거래소 ID — WebSocket 구독 토픽(`/topic/tickers.{exchangeId}`)에 사용 |
| baseCurrencyBalance | BigDecimal | 기축통화(KRW/USDT) 잔고 |
| baseCurrencySymbol | String | 기축통화 심볼 |

**holdings[]**

| 필드 | 타입 | 설명 |
|------|------|------|
| coinId | Long | 코인 ID |
| coinSymbol | String | 코인 심볼 (BTC, ETH 등) |
| coinName | String | 코인 한국어명 (비트코인, 이더리움 등) |
| quantity | BigDecimal | 보유수량 |
| avgBuyPrice | BigDecimal | 평균매수가 |
| currentPrice | BigDecimal | 현재가 |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| WALLET_NOT_FOUND | 404 | 지갑을 찾을 수 없음 |
| WALLET_NOT_OWNED | 403 | 지갑 소유자가 아님 |

# 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant Controller as PortfolioController
    participant Service as GetMyHoldingsService
    participant GetOwnerId as GetWalletOwnerIdUseCase
    participant FindWallet as FindWalletUseCase
    participant FindExchange as FindExchangeDetailUseCase
    participant GetBalance as GetAvailableBalanceUseCase
    participant FindEvaluated as FindEvaluatedHoldingsUseCase
    participant FindCoinInfo as FindCoinInfoUseCase
    participant MySQL

    Client->>Controller: GET /api/users/{userId}/wallets/{walletId}/portfolio
    Controller->>Service: getMyHoldings(query)

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 01 소유권 검증
    end
    Service->>GetOwnerId: getWalletOwnerId(walletId)
    GetOwnerId->>MySQL: SELECT round.user_id
    GetOwnerId-->>Service: ownerId
    Note over Service: ownerId == userId 검증

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 02 지갑 조회
    end
    Service->>FindWallet: findById(walletId)
    FindWallet->>MySQL: SELECT wallet
    FindWallet-->>Service: WalletResult (exchangeId)

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 03 거래소 기축통화 조회
    end
    Service->>FindExchange: findExchangeDetail(exchangeId)
    FindExchange->>MySQL: SELECT exchange
    FindExchange-->>Service: ExchangeDetailResult (baseCurrencyCoinId)

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 04 기축통화 잔고 조회
    end
    Service->>GetBalance: getAvailableBalance(walletId, baseCurrencyCoinId)
    GetBalance->>MySQL: SELECT available
    GetBalance-->>Service: baseCurrencyBalance

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 05 평가된 보유 자산 조회
    end
    Service->>FindEvaluated: findEvaluatedHoldings(walletId, exchangeId)
    FindEvaluated->>MySQL: SELECT holdings + live price
    FindEvaluated-->>Service: List<EvaluatedHoldingResult>

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 06 코인 심볼·이름 조회
    end
    Service->>FindCoinInfo: findByIds(coinIds)
    FindCoinInfo->>MySQL: SELECT coins
    FindCoinInfo-->>Service: Map<Long, CoinInfoResult>

    Service-->>Controller: MyHoldingsResult
    Controller-->>Client: 200 OK
```
