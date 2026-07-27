## API 명세

### 참고사항

- 이 API는 순수 조회이다. 복기 리포트가 사전에 생성되어 있어야 한다
- 자산 추이 데이터는 일별 단위로 반환한다. 그래프 세밀도 조절(x축 라벨 간격 등)은 클라이언트에서 처리한다
- 거래소별로 요청한다

### 원칙 골라 보기의 역할 분담

- 서버는 전체 원칙을 준수했을 때의 시뮬레이션 자산(`ruleFollowedAsset`)만 내려준다. 원칙 조합별 자산은 미리 계산하지 않는다. 가능한 조합의 수가 많고, 계산에 필요한 재료(원칙별 위반 손실과 발생일)는 복기 리포트 응답에 이미 모두 들어 있기 때문이다
- 원칙을 골라 켠 곡선은 클라이언트가 만든다. 복기 리포트의 위반 거래 목록에서 켜 둔 원칙 몫의 위반 손실만 골라 발생일 순으로 누적하고, 이를 그날의 실제 자산에 더한다. 서버가 `ruleFollowedAsset` 을 만드는 방식과 같은 계산이다

`GET /api/rounds/{roundId}/regret/chart?exchangeId={exchangeId}`

### Path Parameter

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roundId | Long | O | 투자 라운드 ID |

### Query Parameter

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| exchangeId | Long | O | 거래소 ID |

### Request

```
GET /api/rounds/1/regret/chart?exchangeId=1
```

### Response

```json
{
  "status": 200,
  "code": "OK",
  "message": "복기 그래프 데이터를 조회했습니다.",
  "data": {
    "roundId": 1,
    "exchangeId": 1,
    "exchangeName": "업비트",
    "currency": "KRW",
    "totalDays": 42,

    "assetHistory": [
      {
        "snapshotDate": "2026-01-15",
        "actualAsset": 10000000,
        "ruleFollowedAsset": 10000000,
        "btcHoldAsset": 10000000
      },
      {
        "snapshotDate": "2026-01-22",
        "actualAsset": 10150000,
        "ruleFollowedAsset": 10535000,
        "btcHoldAsset": 10230000
      },
      {
        "snapshotDate": "2026-02-25",
        "actualAsset": 10400000,
        "ruleFollowedAsset": 11293837,
        "btcHoldAsset": 11180000
      }
    ],

    "violationMarkers": [
      {
        "snapshotDate": "2026-01-22",
        "assetValue": 10150000
      },
      {
        "snapshotDate": "2026-02-10",
        "assetValue": 10200000
      }
    ]
  }
}
```

### 응답 필드 상세

#### 최상위 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| roundId | Long | 투자 라운드 ID |
| exchangeId | Long | 거래소 ID |
| exchangeName | String | 거래소 이름 |
| currency | String | 기축통화 (KRW, USDT) |
| totalDays | Integer | 분석 기간 총 일수. 첫 스냅샷 ~ 마지막 스냅샷 날짜 차이 + 1 |

#### assetHistory[]

| 필드 | 타입 | 설명 |
|------|------|------|
| snapshotDate | LocalDate | 스냅샷 날짜 |
| actualAsset | BigDecimal | 해당일 실제 총 자산 (기축통화 단위) |
| ruleFollowedAsset | BigDecimal | 해당일 규칙 준수 시 시뮬레이션 총 자산 (기축통화 단위) |
| btcHoldAsset | BigDecimal | 해당일 BTC 홀드 시 자산 (기축통화 단위) |

#### violationMarkers[]

| 필드 | 타입 | 설명 |
|------|------|------|
| snapshotDate | LocalDate | 위반 발생 날짜. 같은 날 여러 위반은 하나로 합친다 |
| assetValue | BigDecimal | 해당 시점의 실제 자산값 (기축통화 단위). 마커의 y좌표 |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ROUND_NOT_FOUND | 404 | 투자 라운드를 찾을 수 없음 |
| ROUND_ACCESS_DENIED | 403 | 본인의 라운드가 아님 |
| EXCHANGE_NOT_FOUND | 404 | 해당 거래소가 라운드에 존재하지 않음 |
| REPORT_NOT_FOUND | 404 | 복기 리포트가 아직 생성되지 않음 |
| SNAPSHOT_NOT_FOUND | 404 | 포트폴리오 스냅샷이 아직 생성되지 않음 |
