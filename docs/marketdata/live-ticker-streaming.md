# 실시간 티커 스트리밍

## 개요

거래소별 실시간 티커(현재가, 등락률, 거래대금)를 WebSocket으로 클라이언트에 push한다. RabbitMQ fanout exchange를 통해 시세 이벤트를 수신한다.

시세 수집기가 거래소 WebSocket에서 수신한 정규화 티커를 Redis에 캐싱하고, 동시에 RabbitMQ fanout exchange로 발행한다. 각 서버는 자기 anonymous 큐에서 메시지를 소비하여 STOMP 토픽으로 클라이언트에 전달한다.

마켓 탭, 포트폴리오 탭, 입출금 탭 등 실시간 시세가 필요한 모든 화면이 이 토픽을 구독한다. 각 화면은 필요한 필드만 선택적으로 사용한다.

## 메시징 인프라: RabbitMQ fanout

시세 이벤트는 RabbitMQ `ticker.exchange` fanout exchange를 통해 전파된다. 두 컨텍스트가 독립적으로 소비한다:

- **marketdata**: `ticker.marketdata.{uuid}` 큐 → WebSocket 브로드캐스트
- **trading**: `ticker.trading.{uuid}` 큐 → 미체결 주문 매칭

각 큐는 anonymous(exclusive, auto-delete)로 생성되어 서버 재시작 시 자동 정리된다.

## 메시지 흐름

1. 시세 수집기가 거래소 WebSocket에서 티커를 수신한다
2. RabbitMQ fanout exchange로 `TickerMessage`를 발행한다
3. marketdata `LiveTickerEventListener`가 `ticker.marketdata.{uuid}` 큐에서 메시지를 소비한다
4. `ResolveLiveTickerService`가 매핑 캐시에서 exchange+symbol → ExchangeCoinMapping을 조회하여 `LiveTickerResult`를 반환한다
5. `LiveTickerEventListener`가 `LivePriceResponse`로 변환하여 `/topic/prices.{exchangeId}`로 전송한다

## 워밍업

`MarketdataWarmupInitializer`가 `ApplicationReadyEvent`에서:
1. `WarmupExchangeCoinMappingUseCase.warmup()`으로 exchange+symbol → ExchangeCoinMapping 캐시를 로딩한다
2. `tickerMarketdataListener` RabbitMQ 리스너를 시작한다

## STOMP 토픽

```
/topic/prices.{exchangeId}
```

- 거래소별 토픽으로, 해당 거래소의 모든 코인 티커 업데이트가 개별 메시지로 전달된다
- 클라이언트는 현재 보고 있는 거래소 토픽 1개만 구독한다
- 거래소 탭 전환 시 기존 구독 해제 + 새 거래소 구독

## 메시지 포맷

### TickerMessage (RabbitMQ 수신)

```json
{
  "exchange": "Upbit",
  "symbol": "BTC/KRW",
  "currentPrice": 143250000,
  "changeRate": 0.0234,
  "quoteTurnover": 892400000000,
  "timestamp": 1709913600000
}
```

### LivePriceResponse (WebSocket 전송)

```json
{
  "coinId": 1,
  "symbol": "BTC",
  "price": 143250000,
  "changeRate": 0.0234,
  "quoteTurnover": 892400000000,
  "timestamp": 1709913600000
}
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

클라이언트가 `/topic/prices.{exchangeId}`를 구독하면, 아래 화면들이 수신된 메시지에서 필요한 필드를 선택적으로 사용한다.

| 화면 | 사용 필드 | 용도 |
|------|----------|------|
| 마켓 탭 | price, changeRate, quoteTurnover | 코인 목록 실시간 시세 표시 |
| 포트폴리오 투자 현황 | price | 보유 코인 실시간 평가 |
