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
