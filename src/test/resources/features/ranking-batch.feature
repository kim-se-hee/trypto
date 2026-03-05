# language: ko
기능: 랭킹 배치

  시나리오: 스냅샷 데이터 기반 랭킹 계산
    먼저 랭킹 배치 데이터를 초기화한다
    그리고 랭킹 대상 라운드가 존재한다
      | userId | roundId | tradeCount |
      | 1      | 1       | 5          |
      | 2      | 2       | 3          |
    그리고 스냅샷 데이터가 존재한다
      | userId | roundId | exchangeId | totalAssetKrw | totalInvestmentKrw |
      | 1      | 1       | 1          | 11000000      | 10000000           |
      | 2      | 2       | 1          | 9500000       | 10000000           |
    그리고 비교 스냅샷 데이터가 존재한다
      | userId | roundId | exchangeId | totalAssetKrw | totalInvestmentKrw |
      | 1      | 1       | 1          | 10000000      | 10000000           |
      | 2      | 2       | 1          | 10000000      | 10000000           |
    만일 랭킹 배치를 실행한다
    그러면 랭킹 배치가 COMPLETED 상태이다
    그리고 DAILY 랭킹이 2건 생성된다
    그리고 1위의 수익률은 10.0000이다
    그리고 2위의 수익률은 -5.0000이다
