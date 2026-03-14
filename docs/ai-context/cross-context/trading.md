# trading 크로스 컨텍스트 인터페이스

패키지: `ksh.tryptobackend.trading.application.port.in`

## UseCase

### FindActiveHoldingsUseCase
- `findActiveHoldings(Long walletId) → List<HoldingInfoResult>`

### FindEvaluatedHoldingsUseCase
- `findEvaluatedHoldings(Long walletId, Long exchangeId) → List<EvaluatedHoldingResult>`

### CountFilledOrdersUseCase
- `existsByWalletId(Long walletId) → boolean`
- `countByWalletId(Long walletId) → int`
- `countGroupByWalletIds(List<Long> walletIds) → Map<Long, Integer>`

### FindFilledOrdersUseCase
- `findByOrderIds(List<Long> orderIds) → List<FilledOrderResult>`
- `findSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after) → List<FilledOrderResult>`

### FindViolationsUseCase
- `findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId) → List<ViolationResult>`

## Result DTO

| DTO | 필드 |
|-----|------|
| HoldingInfoResult | coinId: Long, avgBuyPrice: BigDecimal, totalQuantity: BigDecimal |
| EvaluatedHoldingResult | coinId: Long, avgBuyPrice: BigDecimal, totalQuantity: BigDecimal, currentPrice: BigDecimal |
| FilledOrderResult | orderId: Long, walletId: Long, exchangeCoinId: Long, side: String, amount: BigDecimal, quantity: BigDecimal, filledPrice: BigDecimal, filledAt: LocalDateTime |
| ViolationResult | violationId: Long, orderId: Long, ruleId: Long, createdAt: LocalDateTime |
