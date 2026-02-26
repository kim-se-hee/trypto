package ksh.tryptobackend.portfolio.adapter.out;

import ksh.tryptobackend.portfolio.domain.model.Holding;
import ksh.tryptobackend.trading.application.port.out.HoldingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HoldingJpaPersistenceAdapter implements HoldingPort {

    private final HoldingJpaRepository repository;

    @Override
    public Optional<HoldingData> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId)
            .map(entity -> new HoldingData(
                entity.getAvgBuyPrice(),
                entity.getTotalQuantity(),
                entity.getAveragingDownCount()));
    }

    @Override
    public void applyBuy(Long walletId, Long coinId, BigDecimal filledPrice,
                         BigDecimal filledQuantity, BigDecimal currentPrice) {
        HoldingJpaEntity entity = getOrCreateEntity(walletId, coinId);
        Holding holding = entity.toDomain();
        holding.applyBuy(filledPrice, filledQuantity, currentPrice);
        entity.updateFrom(holding);
    }

    @Override
    public void applySell(Long walletId, Long coinId, BigDecimal filledQuantity) {
        HoldingJpaEntity entity = getOrCreateEntity(walletId, coinId);
        Holding holding = entity.toDomain();
        holding.applySell(filledQuantity);
        entity.updateFrom(holding);
    }

    private HoldingJpaEntity getOrCreateEntity(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId)
            .orElseGet(() -> repository.save(new HoldingJpaEntity(walletId, coinId)));
    }
}
