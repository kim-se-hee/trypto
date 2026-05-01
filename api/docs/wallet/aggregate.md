# Aggregate Root / Entity / Value Object

| Aggregate Root | Entity | Value Object |
|---|---|---|
| Wallet | WalletBalance, DepositAddress | DepositTargetExchange, WalletBalances |

# 소유 관계

- Wallet → WalletBalance, DepositAddress
- WalletBalances → WalletBalance
