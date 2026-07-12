package ksh.tryptobackend.investmentround.adapter.out.acl;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.investmentround.domain.vo.SeedAmountPolicy;
import ksh.tryptobackend.investmentround.domain.vo.SeedFundingSpec;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetPriceChangeRateUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** marketdata 컨텍스트 조회를 investmentround 로 번역하는 ACL 어댑터. */
@Component
@RequiredArgsConstructor
public class InvestmentRoundAclMarketDataQueryAdapter implements MarketDataQueryPort {

    private final GetPriceChangeRateUseCase getPriceChangeRateUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;

    @Override
    public BigDecimal getChangeRate(Long exchangeCoinId) {
        return getPriceChangeRateUseCase.getChangeRate(exchangeCoinId);
    }

    @Override
    public Long getBaseCurrencyCoinId(Long exchangeId) {
        return findExchangeDetailUseCase
                .findExchangeDetail(exchangeId)
                .map(ExchangeDetailResult::baseCurrencyCoinId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    @Override
    public SeedFundingSpec getSeedFundingSpec(Long exchangeId) {
        return findExchangeDetailUseCase
                .findExchangeDetail(exchangeId)
                .map(this::toSeedFundingSpec)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private SeedFundingSpec toSeedFundingSpec(ExchangeDetailResult detail) {
        return new SeedFundingSpec(detail.baseCurrencyCoinId(), SeedAmountPolicy.forDomestic(detail.domestic()));
    }
}
