## 도메인 모델

### Ranking (조회)

- RANKING 테이블에 배치가 적재한 집계 결과를 조회만 한다. 실시간 계산하지 않는다.
- 기간과 최신 기준 날짜로 참여자 수, 최고 수익률, 평균 수익률을 집계한다.

## 타 컨텍스트 의존성

- 없음 (RANKING 테이블 조회만 수행)

## 처리 로직

1. RANKING 테이블에서 `period` + 최신 `referenceDate` 기준으로 집계한다
2. 참여자 수, 최고 수익률, 평균 수익률을 계산한다
3. 해당 기간의 랭킹 데이터가 없으면 `RANKING_NOT_FOUND` 에러를 반환한다

## 설계 포인트

- 인증 불필요 (공개 통계)
- 상위 100명 데이터이므로 실시간 집계도 부담 없음
- `referenceDate`는 내부적으로 최신 날짜를 자동 결정한다

## 집계 쿼리

```sql
SELECT COUNT(*), MAX(profit_rate), AVG(profit_rate)
FROM ranking
WHERE period = :period
  AND reference_date = :referenceDate
```

## 시퀀스 플로우

```mermaid
sequenceDiagram
    participant Client
    participant Controller as RankingController
    participant Service as GetRankingStatsService
    participant RankingAdapter as RankingPersistenceAdapter
    participant MySQL

    Client->>Controller: GET /api/rankings/stats?period=DAILY
    Controller->>Service: getRankingStats(query)

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 01 최신 기준 날짜 결정
    end
    Service->>RankingAdapter: findLatestReferenceDate(period)
    RankingAdapter->>MySQL: SELECT MAX(reference_date) WHERE period = ?
    RankingAdapter-->>Service: latestDate

    alt 랭킹 데이터 없음
        Service-->>Controller: RANKING_NOT_FOUND 에러
        Controller-->>Client: 404 Not Found
    end

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 02 통계 집계
    end
    Service->>RankingAdapter: getRankingStats(period, latestDate)
    RankingAdapter->>MySQL: SELECT COUNT(*), MAX(profit_rate), AVG(profit_rate)
    RankingAdapter-->>Service: RankingStatsResult

    Service-->>Controller: RankingStatsResult
    Controller-->>Client: 200 OK
```

## task 목록

- [ ] 기간(period) 검증 로직 구현(`DAILY`/`WEEKLY`/`MONTHLY` 외 값은 `INVALID_RANKING_PERIOD`)
- [ ] 최신 기준 날짜 조회 어댑터 구현(`findLatestReferenceDate`)
- [ ] 데이터 없음 시 `RANKING_NOT_FOUND` 처리
- [ ] 통계 집계 어댑터 구현(참여자 수·최고 수익률·평균 수익률)
- [ ] 랭킹 통계 조회 UseCase와 서비스 구현
- [ ] 랭킹 통계 조회 REST 어댑터와 응답 DTO

## API 명세

`GET /api/rankings/stats`

### Request Parameters (Query String)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| period | String | O | `DAILY` \| `WEEKLY` \| `MONTHLY` |

### Request

```
GET /api/rankings/stats?period=DAILY
```

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "랭킹 통계를 조회했습니다.",
  "data": {
    "totalParticipants": 100,
    "maxProfitRate": 45.52,
    "avgProfitRate": 7.31
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| INVALID_RANKING_PERIOD | 400 | 유효하지 않은 기간 값 |
| RANKING_NOT_FOUND | 404 | 해당 기간의 랭킹 데이터가 없음 |
