# 실시간 시세 스트리밍

## 개요

거래소별 실시간 시세를 WebSocket으로 클라이언트에 push한다. 크로스 서버 전파에 Redis Pub/Sub을 사용한다.

## 크로스 서버 전파: Redis Pub/Sub 선택 이유

RabbitMQ는 주문 체결 등 다른 이벤트 처리에 활용하지만, 시세 스트리밍에는 Redis Pub/Sub이 더 적합하다:

- 시세 수집기가 거래소 WebSocket에서 실시간(ms 단위)으로 가격을 수신한다
- 시세 수집기가 Redis SET(캐시)과 Redis PUBLISH(실시간 전파)를 동시에 수행한다
- 각 서버가 Redis SUBSCRIBE로 메시지를 수신하고 자기 클라이언트에게 전달한다

Redis Pub/Sub이 크로스 서버 메시지 전파를 담당하므로, 각 서버는 SimpleBroker만으로 로컬 클라이언트에게 전달할 수 있다.

**한 틱 누락 허용:** Redis Pub/Sub은 fire-and-forget이라 서버 일시 단절 시 메시지가 유실될 수 있다. 그러나 시세는 연속적으로 흐르므로 한 틱을 놓쳐도 다음 틱이 즉시 도착한다. 모의투자 플랫폼에서 이는 문제가 되지 않는다.

## 스케일 아웃 아키텍처

```
거래소 WebSocket → 시세 수집기 → Redis PUBLISH (Pub/Sub)
                                       │
                        ┌──────────────┼──────────────┐
                        ↓              ↓              ↓
                   Server A        Server B       Server C
                 (SUBSCRIBE)     (SUBSCRIBE)    (SUBSCRIBE)
                       ↓              ↓              ↓
                 SimpleBroker   SimpleBroker   SimpleBroker
                       ↓              ↓              ↓
                WS Clients A   WS Clients B   WS Clients C
```

### 메시지 흐름

1. 시세 수집기가 거래소 WebSocket에서 가격을 수신한다
2. Redis PUBLISH로 가격 변경을 알린다
3. 각 서버가 Redis SUBSCRIBE로 메시지를 수신한다
4. 서버가 `SimpMessagingTemplate`으로 SimpleBroker에 전달한다
5. SimpleBroker가 해당 토픽을 구독 중인 WebSocket 클라이언트에게 전달한다

## Redis Pub/Sub 채널

```
PUBLISH prices.{exchangeId} {message}
```

각 서버는 `SUBSCRIBE prices.{exchangeId}`로 구독하고, 수신한 메시지를 SimpleBroker를 통해 WebSocket 클라이언트에게 전달한다.

## STOMP 토픽

```
/topic/prices.{exchangeId}
```

- 거래소별 토픽으로, 해당 거래소의 모든 코인 가격 업데이트가 개별 메시지로 전달된다
- 클라이언트는 현재 보고 있는 거래소 토픽 1개만 구독한다
- 거래소 탭 전환 시 기존 구독 해제 + 새 거래소 구독

**Redis Pub/Sub 채널 → STOMP 토픽 매핑**

| Redis 채널 | STOMP 토픽 | 설명 |
|-----------|-----------|------|
| `prices.{exchangeId}` | `/topic/prices.{exchangeId}` | 서버가 Redis 메시지를 수신하여 STOMP 토픽으로 전달 |

## 메시지 포맷

```json
{
  "coinId": 1,
  "symbol": "BTC",
  "price": 143250000,
  "changeRate": 2.3,
  "timestamp": 1709913600000
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| coinId | Long | 코인 ID |
| symbol | String | 코인 심볼 |
| price | BigDecimal | 현재가 (거래소 기축통화 단위) |
| changeRate | BigDecimal | 등락률 (%) — 업비트/빗썸: 전일종가 대비, 바이낸스: 24h 대비 |
| timestamp | Long | 시세 수신 시각 (epoch ms) |

## Redis Pub/Sub 리스너 구성

```
RedisMessageListenerContainer
  → MessageListener (prices.{exchangeId} 채널 구독)
    → SimpMessagingTemplate.convertAndSend("/topic/prices.{exchangeId}", message)
```

## 소비 기능

| 기능 | 구독 토픽 | 용도 |
|------|----------|------|
| 포트폴리오 투자 현황 | `/topic/prices.{exchangeId}` | 보유 코인 실시간 평가 |
