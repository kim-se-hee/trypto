매칭 엔진이 체결을 확정한 뒤 api로 알리는 채널.

# 채널

| 항목 | 값 |
|------|------|
| 종류 | RabbitMQ Fanout Exchange |
| 이름 | `order.filled.notification` (`engine.outbox.fanout-exchange`로 외부화) |
| 발행자 | `engine` — `ksh.tryptoengine.outbox.OutboxRelay` |
| 소비자 | `api` — `ksh.tryptobackend.trading.adapter.in.EngineOrderFilledListener` |
| Content-Type | `application/json` (Spring AMQP `JacksonJsonMessageConverter`) |
| Routing key | `""` (fanout) |
| Durable | exchange durable, 소비자 큐는 비-durable / exclusive / auto-delete |

# 페이로드 — OrderFilled

```json
{
  "orderId":      12345,
  "executedPrice":"152300000",
  "quantity":     "0.0125",
  "executedAt":   "2025-12-31T23:59:01.500"
}
```

| 필드 | 약속 |
|------|------|
| `orderId` | 멱등 키 |
| `executedPrice` | quote 통화 단위 체결 단가 |
| `quantity` | base 단위. **이번 체결분만** (누적값 아님) |
| `executedAt` | ISO-8601 LocalDateTime. **engine JVM 로컬 타임존** |
