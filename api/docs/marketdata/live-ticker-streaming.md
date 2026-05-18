# 실시간 티커 스트리밍

## 개요

거래소별 실시간 티커(현재가, 등락률, 거래대금)를 WebSocket으로 클라이언트에 push한다. RabbitMQ fanout exchange를 통해 시세 이벤트를 수신한다.

시세 수집기가 거래소 WebSocket에서 수신한 정규화 티커를 Redis에 캐싱하고, 동시에 RabbitMQ fanout exchange로 발행한다. 각 서버는 자기 anonymous 큐에서 메시지를 소비하여 STOMP 토픽으로 클라이언트에 전달한다.

마켓 탭, 포트폴리오 탭, 입출금 탭 등 실시간 시세가 필요한 모든 화면이 이 토픽을 구독한다. 각 화면은 필요한 필드만 선택적으로 사용한다.

## 메시징 인프라: RabbitMQ fanout

시세 이벤트는 RabbitMQ `ticker.exchange` fanout exchange를 통해 전파된다. 두 컨텍스트가 독립적으로 소비한다:

- **marketdata**: `ticker.marketdata.{uuid}` 큐 → WebSocket 브로드캐스트
- **trading**: `ticker.trading.{uuid}` 큐 → 미체결 주문 매칭

큐 단위는 api 인스턴스다. RabbitMqConfig 가 @Bean 으로 anonymous 큐 1개를 선언하므로, fanout exchange 는 api 인스턴스 수만큼만 복제한다. 큐 이름의 {uuid} 는 인스턴스 부팅 시 1회 생성되어 다중 인스턴스 환경에서 이름이 충돌하지 않도록 한다.

각 큐는 anonymous(exclusive, auto-delete)로 생성되어 서버 재시작 시 자동 정리된다.

## 메시지 흐름

1. 시세 수집기가 거래소 WebSocket에서 티커를 수신한다
2. collector 의 `TickerEventConflator` 가 `(exchange, base, quote)` 별 slot 에 최신 1건만 유지하다 50ms 마다 거래소별로 묶어 RabbitMQ fanout exchange 로 `TickerBatchMessage` 1건을 발행한다 (= 1 메시지 / 1 거래소 / 50ms)
3. marketdata `LiveTickerEventListener`가 `ticker.marketdata.{uuid}` 큐에서 batch 를 소비한다
4. batch 안의 각 ticker 에 대해 `ResolveLiveTickerService`가 exchange+symbol → ExchangeCoinMapping 으로 `LiveTickerResult`를 반환한다
5. `LiveTickerEventListener`가 resolve 결과들을 `List<TickerResponse>` 로 묶어 `/topic/tickers.{exchangeId}` 로 STOMP 1 프레임으로 전송한다

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

스키마 단일 소스는 [docs/contracts/ticker-exchange.md](../../../docs/contracts/ticker-exchange.md).

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
