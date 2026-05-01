다른 컨텍스트에 노출하는 UseCase가 없다.

# 의존

## Wallet
- `FindWalletUseCase` — 송신/수신 지갑 조회
- `GetAvailableBalanceUseCase` — 잔고 검증
- `ManageWalletBalanceUseCase` — 잔고 차감/추가
- `GetWalletOwnerIdUseCase` — 지갑 소유자 확인

## MarketData
- `FindCoinSymbolsUseCase` — 송금 내역 조회 시 코인 심볼 변환
