# 잔고

- 지갑 생성 시 거래소의 기축통화와 전 거래 코인 잔고를 0으로 함께 생성한다. 잔고 변경은 행의 사전 존재를 전제하며, 행이 없으면 `WALLET_BALANCE_NOT_FOUND` 오류다
- 잔고 충분성 불변식(가용·잠금 ≥ 0)은 `WalletBalance` 애그리거트가 지킨다. 위반 시 `INSUFFICIENT_BALANCE`
- 잔고 변경은 비관적 잠금으로 조회한 애그리거트에 public 메소드로 수행한다. 여러 행을 잠글 때는 (walletId, coinId) 오름차순으로 잠가 교착을 방지한다

# 송금

## 범위

- 국내 거래소(업비트, 빗썸)와 해외 거래소(바이낸스) 간 양방향 코인 송금이 가능하다
- KRW(법정화폐)는 송금할 수 없다. USDT는 암호화폐이므로 송금 가능하다

## 송금 절차

1. 출발 거래소에서 출금을 요청한다 (도착 지갑(toWalletId) 지정, 수량 입력)

## 멱등성

- 클라이언트가 `clientTransferId`(UUID)를 생성하여 전송한다
- 서버는 동일한 `clientTransferId`로 중복 요청이 들어오면 기존 송금 결과를 반환하고 새 송금을 생성하지 않는다

## 잔고 변동

- 성공 시: 출발 지갑에서 `deductAvailable(amount)`, 도착 지갑에서 `addAvailable(amount)`
