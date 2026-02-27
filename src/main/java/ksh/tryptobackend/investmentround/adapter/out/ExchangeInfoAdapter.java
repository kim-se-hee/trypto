package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.ExchangeInfoPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.ExchangeInfo;
import ksh.tryptobackend.investmentround.domain.vo.SeedAmountPolicy;
import ksh.tryptobackend.marketdata.adapter.out.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeInfoAdapter implements ExchangeInfoPort {

    private final ExchangeJpaRepository exchangeJpaRepository;

    @Override
    public Optional<ExchangeInfo> findById(Long exchangeId) {
        return exchangeJpaRepository.findById(exchangeId)
            .map(this::toExchangeInfo);
    }

    private ExchangeInfo toExchangeInfo(ExchangeJpaEntity entity) {
        return new ExchangeInfo(
            entity.getBaseCurrencyCoinId(),
            toSeedAmountPolicy(entity.getMarketType())
        );
    }

    private SeedAmountPolicy toSeedAmountPolicy(ExchangeMarketType marketType) {
        return switch (marketType) {
            case DOMESTIC -> SeedAmountPolicy.DOMESTIC;
            case OVERSEAS -> SeedAmountPolicy.OVERSEAS;
        };
    }
}
