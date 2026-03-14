# 개요

입출금 탭은 거래소 간 코인 송금과 잔고 확인을 담당한다. 잔고·이체 내역은 매번 fresh fetch하고, 체인 목록·출금 수수료·입금 주소 같은 정적 데이터는 캐싱한다.

# 화면 구성

| 영역 | 설명 |
|------|------|
| 거래소 탭 | 업비트 / 빗썸 KRW / 바이낸스 USDT (walletId로 전환, 기축통화 표시) |
| 총 자산 요약 | 선택된 거래소의 총 자산(기축통화 환산), 보유 기축통화(사용 가능 / 잠금). 업비트·빗썸은 KRW, 바이낸스는 USDT |
| 보유 자산 목록 | 코인(심볼·한글명), 보유수량(≈ KRW 환산), 사용가능, 잠금. 검색·소액 제외 필터, 컬럼별 정렬 |
| 출금 폼 | 코인 선택 → 네트워크 선택 → 주소·태그 입력 → 수량 입력 → 수수료 확인 → 출금 |
| 입금 영역 | 코인·네트워크 선택 → 입금 주소·태그 표시 |
| 이체 내역 | 입금/출금 이력 (커서 기반 페이지네이션) |

# 데이터 소스

| 데이터 | 소스 | 갱신 주기 | 캐싱 |
|--------|------|----------|------|
| walletId ↔ exchangeId 매핑 | 활성 라운드 API | 라운드 시작 시 1회 | O |
| 거래소 상장 코인 목록 | 거래소 코인 목록 API | 앱 로딩 시 1회 | O (마켓탭과 공유) |
| 잔고 (available, locked) | 잔고 조회 API | 탭 진입·이벤트 수신 시 fresh fetch, 송금 후 로컬 갱신 | X |
| 체인 목록 (exchangeCoinChain) | 체인 목록 API | 코인 선택 시 | O (exchangeId + coinId 키) |
| 출금 수수료 (fee, minWithdrawal) | 출금 수수료 API | 네트워크 선택 시 | O (exchangeId+coinId+chain 키) |
| 입금 주소 (address, tag) | 입금 주소 API | 네트워크 선택 시 | O (한 번 발급되면 불변) |
| 이체 내역 | 이체 내역 API | 탭 진입 시 fresh fetch | X |
| 현재가 | WebSocket 티커 | 실시간 | X |

## 캐싱 가능 데이터

| 데이터 | API | staleTime | 캐시 키 | 근거 |
|--------|-----|-----------|---------|------|
| 거래소 코인 목록 | `GET /api/exchanges/{exchangeId}/coins` | 수 시간 | exchangeId | 마스터 데이터, 마켓탭과 공유 |
| 체인 목록 | `GET /api/exchanges/{exchangeId}/coins/{coinId}/chains` | 수 시간 | exchangeId + coinId | 마스터 데이터, 운영자만 변경 |
| 출금 수수료 | `GET /api/withdrawal-fees` | 1시간 | exchangeId + coinId + chain | 운영자 설정, 거의 안 변함 |
| 입금 주소 | `GET /api/wallets/{walletId}/deposit-address` | Infinity (무한) | walletId + chain | 한 번 발급되면 변하지 않음 |
| walletId 매핑 | `GET /api/rounds/active` | 라운드 시작 시 무효화 | userId | 라운드 시작 시에만 변경 |

## 캐싱 불가 데이터

| API | 이유 |
|-----|------|
| `GET /api/users/{userId}/wallets/{walletId}/balances` | 주문 체결·송금으로 수시 변동 |
| `GET /api/wallets/{walletId}/transfers` | 새 이체 발생 시 즉시 반영 필요 |

# API 의존

| API | 문서 | 용도 |
|-----|------|------|
| `GET /api/rounds/active?userId=` | [active-round.md](../investmentround/active-round.md) | walletId 목록 |
| `GET /api/exchanges/{exchangeId}/coins` | [find-exchange-coins.md](../marketdata/find-exchange-coins.md) | 코인 심볼·이름 (캐싱) |
| `GET /api/exchanges/{exchangeId}/coins/{coinId}/chains` | [find-coin-chains.md](../marketdata/find-coin-chains.md) | 체인 목록 (캐싱) |
| `GET /api/users/{userId}/wallets/{walletId}/balances` | [wallet-assets.md](../wallet/wallet-assets.md) | 잔고 조회 |
| `GET /api/withdrawal-fees` | [withdrawal-fee-api.md](../marketdata/withdrawal-fee-api.md) | 출금 수수료 (캐싱) |
| `GET /api/wallets/{walletId}/deposit-address` | [deposit-address.md](../wallet/deposit-address.md) | 입금 주소 (캐싱) |
| `POST /api/transfers` | [transfer.md](../transfer/transfer.md) | 송금 실행 |
| `GET /api/wallets/{walletId}/transfers` | [transfer-history.md](../transfer/transfer-history.md) | 이체 내역 |
| WebSocket `/topic/tickers.{exchangeId}` | [live-ticker-streaming.md](../marketdata/live-ticker-streaming.md) | 실시간 시세 |

# WebSocket 의존

## 실시간 시세 (브로드캐스트)

STOMP 토픽: `/topic/tickers.{exchangeId}`

- 보유 자산 목록에서 평가금액 실시간 갱신에 사용한다
- 마켓탭·포트폴리오탭과 동일한 토픽을 공유한다

## 사용자 이벤트 (개인 채널)

STOMP user destination: `/user/queue/events`

- 서버에서 사용자별 비동기 이벤트를 푸시한다
- 입출금 탭에서 수신해야 하는 이벤트:

| eventType | 발생 시점 | 프론트 동작 |
|-----------|----------|-----------|
| `ORDER_FILLED` | 지정가 주문 체결 (매칭 스케줄러) | 잔고 API refetch (잔고 변동) |
| `FROZEN_FUNDS_RELEASED` | 동결 자금 24시간 후 자동 반환 (배치) | 잔고 API + 이체 내역 API refetch |

- 메시지 형식: `{eventType, walletId, ...}`
- 현재 walletId와 일치할 때만 refetch 트리거

# 프론트 조합 흐름

## 보유 자산 목록

```
1. 앱 초기화 (캐싱)
   → GET /api/rounds/active → wallets: [{walletId, exchangeId}, ...]
   → GET /api/exchanges/{exchangeId}/coins → coinMap (캐시 히트 시 스킵)

2. 입출금 탭 진입
   → GET /api/users/{userId}/wallets/{walletId}/balances → 잔고 목록
   → SUBSCRIBE /topic/tickers.{exchangeId} → 실시간 시세

3. 프론트 state
   - coinMap: Map<coinId, {exchangeCoinId, symbol, name}>  ← 캐싱
   - balanceMap: Map<coinId, {available, locked}>           ← fresh fetch
   - baseCurrency: {symbol, available, locked}              ← fresh fetch
   - tickerMap: Map<coinId, {price, ...}>                   ← WebSocket

4. 총 자산 요약 렌더링
   - 총 자산 = baseCurrency.available + baseCurrency.locked + Σ(보유수량 × tickerMap[coinId].price)
   - 단위: 업비트·빗썸은 KRW(원), 바이낸스는 USDT(달러)
   - 보유 기축통화: baseCurrency.available + baseCurrency.locked
   - 사용 가능 / 잠금: baseCurrency.available / baseCurrency.locked
   - 눈 아이콘으로 금액 표시/숨김 토글

5. 보유 자산 목록 렌더링
   - balanceMap에서 available + locked > 0인 코인만 표시 (보유 코인만)
   - 컬럼: 코인(심볼·한글명), 보유수량, 사용가능, 잠금
   - 보유수량 = available + locked, 하위에 ≈ 기축통화 환산값 표시 (보유수량 × tickerMap[coinId].price)
   - 잠금: locked > 0이면 🔒 아이콘 + 수량, 0이면 "—" 표시
   - 코인 검색: symbol 또는 name으로 필터링
   - 소액 제외: 기축통화 환산 평가금액이 기준 이하인 코인 숨김
   - 정렬: 코인명(가나다), 보유수량(기축통화 환산), 사용가능, 잠금 컬럼별 오름/내림차순
```

## 출금 흐름

```
1. 코인 선택
   → 보유 자산 목록에서 코인 선택 (coinId 확정)

2. 네트워크 선택
   → GET /api/exchanges/{exchangeId}/coins/{coinId}/chains → 체인 목록 (캐싱)
   → 사용자가 네트워크 선택 (chain 확정)

3. 수수료 확인
   → GET /api/withdrawal-fees?exchangeId=&coinId=&chain= → fee, minWithdrawal (캐싱)
   → 수수료와 최소 출금 수량을 화면에 표시

4. 주소·태그 입력
   → 사용자가 도착 주소와 태그(필요 시) 직접 입력

5. 수량 입력 + 검증 (프론트)
   → amount ≥ minWithdrawal 확인
   → available ≥ amount + fee 확인

6. 출금 제출
   → POST /api/transfers
   → 성공/동결 결과 표시
   → 응답 데이터로 잔고 로컬 갱신 (amount + fee 차감)
   → 응답의 TransferCoinResponse를 이체 내역 목록 맨 앞에 prepend (type=WITHDRAW, fee는 원본 그대로)
```

## 입금 흐름

```
1. 코인·네트워크 선택
   → 체인 목록 조회 (캐싱)

2. 입금 주소 조회
   → GET /api/wallets/{walletId}/deposit-address?coinId=&chain= (캐싱)
   → 주소·태그 표시 + 복사 기능

3. 안내 문구
   → "이 주소로 다른 거래소에서 출금하세요"
   → 태그가 있으면 "태그를 반드시 입력하세요" 경고
```

## 이체 내역

```
1. 입출금 탭 하단 또는 별도 서브탭
   → GET /api/wallets/{walletId}/transfers?type=ALL → 최초 로드
   → 스크롤 시 cursor 기반 추가 로드

2. 필터: 전체 / 입금(DEPOSIT) / 출금(WITHDRAW)
   → type 파라미터로 서버 필터링

3. 표시 정보
   - type (입금/출금), coinId → coinSymbol (캐싱 coinMap으로 매핑)
   - chain, amount, fee, status, failureReason, createdAt
   - FROZEN 상태면 frozenUntil 표시 + "자동 반환 예정" 안내
```

# 거래소 탭 전환

```
1. 기존 WebSocket 구독 해제
2. walletMap에서 새 walletId 조회
3. 잔고 API fresh fetch
4. 이체 내역 API fresh fetch
5. 새 거래소 토픽 구독
6. coinMap은 캐시 히트 (마켓탭에서 이미 로드)
```

# 미결 사항

| 항목 | 상태 | 비고 |
|------|------|------|
| 송금 후 자동 갱신 | 설계 완료 | POST 응답 데이터로 잔고 로컬 갱신 + 이체 내역 prepend (refetch 없음) |
| 동결 자금 해제 알림 | 설계 완료 | `/user/queue/events`의 `FROZEN_FUNDS_RELEASED` 이벤트로 잔고 + 이체 내역 refetch |
