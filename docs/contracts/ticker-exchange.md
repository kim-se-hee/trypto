거래소 시세 정규화 결과를 실시간 브로드캐스트하는 채널.

# 채널

| 항목 | 값 |
|------|------|
| 종류 | RabbitMQ Fanout Exchange |
| 이름 | `ticker.exchange` (`app.rabbitmq.ticker-exchange`로 외부화) |
| 발행자 | `collector` — `ksh.tryptocollector.distribute.rabbitmq.TickerEventPublisher` |
| 소비자 | `api` — `ksh.tryptobackend.marketdata.adapter.in.LiveTickerEventListener` |
| Content-Type | `application/json` |
| Routing key | `""` (fanout) |
| Durable | exchange durable, 소비자 큐는 비-durable / exclusive / auto-delete |

# 발행 단위

1 메시지 = **1 거래소의 ticker batch**. 현재 collector 는 tick 1건이 들어오는 즉시 크기 1 batch 로 발행한다. 소비자는 batch 크기를 가정하지 않고 배열로 처리해야 한다.

# 페이로드

```json
{
  "exchange": "UPBIT",
  "tickers": [
    {
      "symbol":        "BTC/KRW",
      "currentPrice":  "152340000",
      "changeRate":    "0.0123",
      "quoteTurnover": "8423199301.55",
      "timestamp":     1735689600123
    },
    {
      "symbol":        "ETH/KRW",
      "currentPrice":  "4500000",
      "changeRate":    "-0.005",
      "quoteTurnover": "1200000000",
      "timestamp":     1735689600145
    }
  ]
}
```

| 필드 | 약속 |
|------|------|
| `exchange` | `UPBIT` / `BITHUMB` / `BINANCE` — batch 단위 1번만 표기 |
| `tickers` | 1개 이상의 ticker. 동일 거래소 안의 서로 다른 symbol 만 포함된다 (같은 key 중복 없음) |
| `tickers[].symbol` | `{base}/{quote}` (예: `BTC/KRW`, `ETH/USDT`) |
| `tickers[].currentPrice` | quote 통화 단위 |
| `tickers[].changeRate` | 24h 변동률. 소수점 (1% = `0.01`) |
| `tickers[].quoteTurnover` | quote 통화 단위 24h 거래대금 |
| `tickers[].timestamp` | epoch **milliseconds** — 거래소에서 수집한 원본 tick 시각 (batch 단위가 아니라 ticker 별) |
