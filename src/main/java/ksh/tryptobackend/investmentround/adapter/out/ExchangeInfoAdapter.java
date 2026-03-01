package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.ExchangeInfoPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.ExchangeInfo;
import ksh.tryptobackend.investmentround.domain.vo.SeedAmountPolicy;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeDetail;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeInfoAdapter implements ExchangeInfoPort {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public Optional<ExchangeInfo> findById(Long exchangeId) {
        return exchangeQueryPort.findExchangeDetailById(exchangeId)
            .map(this::toExchangeInfo);
    }

    private ExchangeInfo toExchangeInfo(ExchangeDetail detail) {
        return new ExchangeInfo(
            detail.baseCurrencyCoinId(),
            toSeedAmountPolicy(detail.marketType())
        );
    }

    private SeedAmountPolicy toSeedAmountPolicy(ExchangeMarketType marketType) {
        return switch (marketType) {
            case DOMESTIC -> SeedAmountPolicy.DOMESTIC;
            case OVERSEAS -> SeedAmountPolicy.OVERSEAS;
        };
    }
}
