# language: ko
기능: 포트폴리오 스냅샷 배치

  시나리오: 활성 라운드 1개, BTC 보유 시 스냅샷 생성
    먼저 스냅샷 배치 데이터를 초기화한다
    그리고 스냅샷용 활성 라운드가 존재한다
      | roundId | userId | exchangeId | walletId | seedAmount |
      | 1       | 1      | 1          | 100      | 10000000   |
    그리고 스냅샷용 거래소 정보가 존재한다
      | exchangeId | baseCurrencyCoinId | conversionRate |
      | 1          | 99                 | DOMESTIC       |
    그리고 스냅샷용 잔고가 존재한다
      | walletId | coinId | balance |
      | 100      | 99     | 5000000 |
    그리고 스냅샷용 보유 종목이 존재한다
      | walletId | exchangeId | coinId | avgBuyPrice | quantity | currentPrice |
      | 100      | 1          | 1      | 90000000    | 0.05     | 95000000     |
    그리고 스냅샷용 긴급자금 합계는 0이다
      | roundId | exchangeId |
      | 1       | 1          |
    만일 스냅샷 배치를 실행한다
    그러면 스냅샷 배치가 COMPLETED 상태이다
    그리고 스냅샷이 1건 생성된다
    그리고 첫 번째 스냅샷의 총자산은 9750000이다
    그리고 첫 번째 스냅샷의 총투자금은 10000000이다
    그리고 첫 번째 스냅샷의 수익률은 -2.5000이다
    그리고 스냅샷 상세가 1건 생성된다
    그리고 첫 번째 상세의 수익률은 5.5600이다

  시나리오: 보유 종목 없이 스냅샷 생성
    먼저 스냅샷 배치 데이터를 초기화한다
    그리고 스냅샷용 활성 라운드가 존재한다
      | roundId | userId | exchangeId | walletId | seedAmount |
      | 2       | 2      | 1          | 200      | 5000000    |
    그리고 스냅샷용 거래소 정보가 존재한다
      | exchangeId | baseCurrencyCoinId | conversionRate |
      | 1          | 99                 | DOMESTIC       |
    그리고 스냅샷용 잔고가 존재한다
      | walletId | coinId | balance |
      | 200      | 99     | 5000000 |
    그리고 스냅샷용 긴급자금 합계는 0이다
      | roundId | exchangeId |
      | 2       | 1          |
    만일 스냅샷 배치를 실행한다
    그러면 스냅샷 배치가 COMPLETED 상태이다
    그리고 스냅샷이 1건 생성된다
    그리고 첫 번째 스냅샷의 총자산은 5000000이다
    그리고 스냅샷 상세가 0건 생성된다

  시나리오: 활성 라운드 2개 시 스냅샷 각각 생성
    먼저 스냅샷 배치 데이터를 초기화한다
    그리고 스냅샷용 활성 라운드가 존재한다
      | roundId | userId | exchangeId | walletId | seedAmount |
      | 3       | 3      | 1          | 300      | 10000000   |
      | 4       | 4      | 1          | 400      | 10000000   |
    그리고 스냅샷용 거래소 정보가 존재한다
      | exchangeId | baseCurrencyCoinId | conversionRate |
      | 1          | 99                 | DOMESTIC       |
    그리고 스냅샷용 잔고가 존재한다
      | walletId | coinId | balance  |
      | 300      | 99     | 10000000 |
      | 400      | 99     | 10000000 |
    그리고 스냅샷용 긴급자금 합계는 0이다
      | roundId | exchangeId |
      | 3       | 1          |
      | 4       | 1          |
    만일 스냅샷 배치를 실행한다
    그러면 스냅샷 배치가 COMPLETED 상태이다
    그리고 스냅샷이 2건 생성된다
