거래소 상장·거래 상태 변화를 collector 가 감지해 api 로 전달하는 채널.

# 채널

| 항목 | 값 |
|------|------|
| 종류 | RabbitMQ Fanout Exchange (durable) |
| 이름 | `market.status` (`app.rabbitmq.market-status-exchange` 로 외부화) |
| 발행자 | `collector` — 상장 상태 동기화 발행자 (신규) |
| 소비자 | `api` — 상태 변화 리스너 (신규) |
| Content-Type | `application/json` |
| Routing key | `""` (fanout) |
| Durable | exchange durable, 소비자 큐 durable · non-exclusive · non-auto-delete (이벤트 유실 방지) |

# 발행 시점

collector 가 3분 주기 동기화에서 상태 전이를 감지한 직후 발행한다. 전이가 없는 회차에는 발행하지 않는다. 감지 규칙은 collector [상장 상태 동기화](../../collector/docs/market-status-sync.md) 참조.

# 페이로드 — MarketStatusChanged

```json
{
  "exchange":    "BINANCE",
  "symbol":      "BTC/USDT",
  "displayName": "BTC",
  "status":      "SUSPENDED"
}
```

| 필드 | 약속 |
|------|------|
| `exchange` | `UPBIT` / `BITHUMB` / `BINANCE` |
| `symbol` | `{base}/{quote}` (예: `BTC/USDT`). 마켓 식별자 |
| `displayName` | 코인 표기명. 신규 상장 시 코인 등록에 사용하며, 이미 있는 코인이면 무시한다 |
| `status` | `TRADING`(거래중) / `SUSPENDED`(거래지원 종료). `SUSPENDED` 는 폐지와 일시 정지를 아우르며 영구를 뜻하지 않는다 |
