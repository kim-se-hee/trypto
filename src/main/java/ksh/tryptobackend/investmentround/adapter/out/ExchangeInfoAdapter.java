package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.ExchangeInfoPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.ExchangeInfo;
import ksh.tryptobackend.investmentround.domain.vo.SeedAmountPolicy;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeInfoAdapter implements ExchangeInfoPort {

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;

    @Override
    public Optional<ExchangeInfo> findById(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(this::toExchangeInfo);
    }

    private ExchangeInfo toExchangeInfo(ExchangeDetailResult detail) {
        return new ExchangeInfo(
            detail.baseCurrencyCoinId(),
            detail.domestic() ? SeedAmountPolicy.DOMESTIC : SeedAmountPolicy.OVERSEAS
        );
    }
}
