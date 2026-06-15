## 메시징 인프라: RabbitMQ fanout

시세 이벤트는 RabbitMQ `ticker.exchange` fanout exchange를 통해 전파된다. 두 컨텍스트가 독립적으로 소비한다:

- **marketdata**: `ticker.marketdata.{uuid}` 큐 → WebSocket 브로드캐스트
- **trading**: `ticker.trading.{uuid}` 큐 → 미체결 주문 매칭

큐 단위는 api 인스턴스다. RabbitMqConfig 가 @Bean 으로 anonymous 큐 1개를 선언하므로, fanout exchange 는 api 인스턴스 수만큼만 복제한다. 큐 이름의 {uuid} 는 인스턴스 부팅 시 1회 생성되어 다중 인스턴스 환경에서 이름이 충돌하지 않도록 한다.

각 큐는 anonymous(exclusive, auto-delete)로 생성되어 서버 재시작 시 자동 정리된다.

## 메시지 흐름

1. collector 가 거래소에서 티커를 수신한다
2. collector 가 거래소·심볼별로 최신 1건만 남기는 50ms 윈도우로 묶어 거래소당 배치 1건을 fanout 발행한다
3. marketdata 가 인스턴스별 큐에서 배치를 소비한다
4. 배치 안의 각 티커를 외부 식별자(거래소+심볼) → 내부 코인 매핑으로 변환한다
5. 변환 결과를 거래소별 STOMP 토픽에 1 프레임으로 내려준다

## 워밍업

`MarketdataWarmupInitializer`가 `ApplicationReadyEvent`에서:
1. `WarmupExchangeCoinMappingUseCase.warmup()`으로 exchange+symbol → ExchangeCoinMapping 캐시를 로딩한다
2. `tickerMarketdataListener` RabbitMQ 리스너를 시작한다

## STOMP 토픽

```
/topic/tickers.{exchangeId}
```

- 거래소별 토픽. 페이로드는 50ms 윈도우 안의 ticker 들을 묶은 **배열** 이다 — 클라이언트는 1 메시지를 받아 forEach 로 갱신한다
- 클라이언트는 현재 보고 있는 거래소 토픽 1개만 구독한다
- 거래소 탭 전환 시 기존 구독 해제 + 새 거래소 구독

## 메시지 포맷

### TickerBatchMessage (RabbitMQ 수신)

```json
{
  "exchange": "UPBIT",
  "tickers": [
    {
      "symbol": "BTC/KRW",
      "currentPrice": 143250000,
      "changeRate": 0.0234,
      "quoteTurnover": 892400000000,
      "timestamp": 1709913600000
    }
  ]
}
```

스키마 단일 소스는 [docs/contracts/ticker-exchange.md](../../../../docs/contracts/ticker-exchange.md).

### `List<TickerResponse>` (WebSocket 전송)

```json
[
  {
    "coinId": 1,
    "symbol": "BTC",
    "price": 143250000,
    "changeRate": 0.0234,
    "quoteTurnover": 892400000000,
    "timestamp": 1709913600000
  }
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| coinId | Long | 코인 ID |
| symbol | String | 코인 심볼 |
| price | BigDecimal | 현재가 (거래소 기축통화 단위) |
| changeRate | BigDecimal | 등락률 (비율) — 업비트/빗썸: 전일종가 대비, 바이낸스: 24h 대비. +1.23%면 0.0123 |
| quoteTurnover | BigDecimal | 24시간 누적 거래대금 (기축통화 단위) |
| timestamp | Long | 시세 수신 시각 (epoch ms) |

## 소비 화면

클라이언트가 `/topic/tickers.{exchangeId}`를 구독하면, 아래 화면들이 수신된 메시지에서 필요한 필드를 선택적으로 사용한다.

| 화면 | 사용 필드 | 용도 |
|------|----------|------|
| 마켓 탭 | price, changeRate, quoteTurnover | 코인 목록 실시간 시세 표시 |
| 포트폴리오 투자 현황 | price | 보유 코인 실시간 평가 |

## task 목록

- [ ] RabbitMQ `ticker.exchange` fanout exchange 와 인스턴스별 anonymous 큐(`ticker.marketdata.{uuid}`) 선언
- [ ] `tickerMarketdataListener` RabbitMQ 리스너로 TickerBatchMessage 소비
- [ ] 배치 내 각 티커를 외부 식별자(거래소+심볼) → 내부 코인 매핑으로 변환
- [ ] 변환 결과를 거래소별 STOMP 토픽(`/topic/tickers.{exchangeId}`)에 배열 1 프레임으로 브로드캐스트
- [ ] `MarketdataWarmupInitializer` 로 ApplicationReadyEvent 에서 매핑 캐시 워밍업 후 리스너 시작

## 이벤트 컨트랙트

시세 이벤트는 RabbitMQ `ticker.exchange` fanout exchange 로 수신한다. TickerBatchMessage 채널 규약과 페이로드 스키마 단일 소스는 [docs/contracts/ticker-exchange.md](../../../../docs/contracts/ticker-exchange.md) 참조.
