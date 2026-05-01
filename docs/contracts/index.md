서비스 간 계약 — 두 개 이상의 모듈이 합의해야 하는 데이터 형태와 통신 규약을 모아둔다. 

## 메시지 채널

- [engine-inbox.md](engine-inbox.md) — 매칭 엔진 인바운드 이벤트(주문 접수/취소·시세 tick)를 직렬화하는 단일 큐
- [ticker-exchange.md](ticker-exchange.md) — 거래소 시세 정규화 결과 실시간 브로드캐스트
- [outbox-events.md](outbox-events.md) — 매칭 엔진 체결 확정 후 api로 전달되는 이벤트

## 공유 저장소 스키마

- [redis-marketdata.md](redis-marketdata.md) — Redis 마켓 데이터(시세 캐시 + 마켓 메타데이터) 키/값 형식 (collector 작성, api 읽기)
- [influx-candle.md](influx-candle.md) — InfluxDB 캔들 measurement 스키마 (collector 작성, api 읽기)
