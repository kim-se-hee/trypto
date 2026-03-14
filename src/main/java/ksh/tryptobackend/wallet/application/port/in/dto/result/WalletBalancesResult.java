package ksh.tryptobackend.wallet.application.port.in.dto.result;

import java.math.BigDecimal;
import java.util.List;

public record WalletBalancesResult(
    Long exchangeId,
    String baseCurrencySymbol,
    BigDecimal baseCurrencyAvailable,
    BigDecimal baseCurrencyLocked,
    List<BalanceDetail> balances
) {

    public record BalanceDetail(Long coinId, BigDecimal available, BigDecimal locked) {
    }
}
