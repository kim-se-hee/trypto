package ksh.tryptobackend.trading.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

public interface HoldingPort {

    Optional<HoldingData> findByWalletIdAndCoinId(Long walletId, Long coinId);

    void applyBuy(Long walletId, Long coinId, BigDecimal filledPrice, BigDecimal filledQuantity,
                  BigDecimal currentPrice);

    void applySell(Long walletId, Long coinId, BigDecimal filledQuantity);

    record HoldingData(
        BigDecimal avgBuyPrice,
        BigDecimal totalQuantity,
        int averagingDownCount
    ) {
    }
}
