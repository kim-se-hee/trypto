# 개요

마켓 탭은 거래소별 전체 코인 시세 목록을 보여준다. 정적 코인 정보(REST API)와 실시간 시세(WebSocket)를 프론트에서 조합하는 구조다. 서버에서 두 데이터를 합쳐서 내려주는 API는 만들지 않는다.

# 화면 구성

| 영역 | 설명 |
|------|------|
| 거래소 탭 | 업비트 KRW / 빗썸 KRW / 바이낸스 USDT |
| 필터 | 전체 / 상승 / 하락 /|
| 코인 목록 | 코인명, 현재가, 전일대비, 7일 차트, 시가총액, 거래대금(24H) |

# 데이터 소스

| 데이터 | 소스 | 갱신 주기 |
|--------|------|----------|
| exchangeCoinId, coinId, symbol, name | REST API (정적) | 앱 로딩 시 1회, 캐싱 |
| currentPrice, changeRate | WebSocket 티커 | 실시간 |
| quoteTurnover (24H 거래대금) | WebSocket 티커 | 실시간 |

## 정적 API

`GET /api/exchanges/{exchangeId}/coins` → 거래소 상장 코인 목록

- 응답: `[{exchangeCoinId, coinId, coinSymbol, coinName}, ...]`
- 상세: [find-exchange-coins.md](../marketdata/find-exchange-coins.md) (별도 브랜치에서 구현)

## 실시간 티커

STOMP 토픽: `/topic/tickers.{exchangeId}`

- 메시지: `{coinId, symbol, price, changeRate, quoteTurnover, timestamp}`
- 상세: [live-ticker-streaming.md](../marketdata/live-ticker-streaming.md)

# 프론트 조합 흐름

```
1. 거래소 탭 선택 (예: 업비트)
   → GET /api/exchanges/1/coins → 정적 코인 목록 (캐싱)
   → SUBSCRIBE /topic/tickers.1 → 실시간 티커 스트림

2. 프론트 state
   - coinMap: Map<exchangeCoinId, {coinId, symbol, name}>  ← 정적 API
   - tickerMap: Map<coinId, {price, changeRate, quoteTurnover, ...}> ← WebSocket

3. 렌더링: coinMap + tickerMap을 coinId로 조인

4. 거래소 탭 전환 시
   → 기존 STOMP 구독 해제
   → 새 거래소 정적 API 호출 (캐시 히트 시 스킵)
   → 새 거래소 토픽 구독
```

# 정렬/필터

모두 클라이언트에서 수행한다. 거래소당 상장 코인 수가 100~200개이므로 서버 정렬/페이지네이션이 불필요하다.

| 필터/정렬 | 기준 | 구현 |
|-----------|------|------|
| 전체 | - | 필터 없음 |
| 상승 | changeRate > 0 | 클라이언트 필터 |
| 하락 | changeRate < 0 | 클라이언트 필터 |
| 거래량순 | quoteTurnover DESC | 클라이언트 정렬 |
| 현재가순 | currentPrice ASC/DESC | 클라이언트 정렬 |
| 전일대비순 | changeRate ASC/DESC | 클라이언트 정렬 |

# 서버에서 조합 API를 만들지 않는 이유

- REST 응답은 스냅샷이므로 내려주는 순간 가격이 변한다
- 결국 실시간 갱신을 위해 WebSocket이 필요하다
- REST(초기) + WebSocket(실시간) 두 채널을 관리하면 복잡도만 증가한다
- 정적 API = 변하지 않는 데이터, WebSocket = 변하는 데이터로 분리하는 것이 깔끔하다

# 미결 사항

| 항목 | 상태 | 비고 |
|------|------|------|
| 7일 미니차트 데이터 | 미설계 | 시계열 DB(InfluxDB) 또는 외부 API 활용 검토 필요 |
| 시가총액 데이터 | 미설계 | 외부 API(CoinGecko 등)에서 주기적 수집 검토 필요 |
