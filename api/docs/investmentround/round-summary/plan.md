## API 명세

`GET /api/rounds/summary`

### 참고사항

- 사용자는 인증 컨텍스트(`@LoginUser`)에서 식별한다.
- `status`, `code`, `message`, `data` 형태의 `ApiResponseDto<T>`를 사용한다.
- 라운드 이력이 없어도 정상 응답이며, 이 경우 누적 횟수는 0이다.

### Request

```http
GET /api/rounds/summary
```

### Response

```json
{
  "status": 200,
  "code": "OK",
  "message": "라운드 요약을 조회했습니다.",
  "data": {
    "totalRoundCount": 3
  }
}
```

### 에러 응답

없음.

## 구조

| 계층 | 구성요소 |
|------|----------|
| adapter/in/web | `RoundController.getRoundSummary()` |
| adapter/in/dto/response | `RoundSummaryResponse` |
| application/port/in | `GetRoundSummaryUseCase` |
| application/port/in/dto/query | `GetRoundSummaryQuery` |
| application/port/in/dto/result | `RoundSummaryResult` |
| application/service | `GetRoundSummaryService` |
| application/port/out | `InvestmentRoundQueryPort.countByUserId()` |
| adapter/out/persistence | `JpaInvestmentRoundQueryAdapter.countByUserId()` |
