package ksh.tryptobackend.investmentround.adapter.out.acl;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.investmentround.domain.vo.KrwConversionRate;
import ksh.tryptobackend.investmentround.domain.vo.SeedAmountPolicy;
import ksh.tryptobackend.investmentround.domain.vo.SeedFundingSpec;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetPriceChangeRateUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvestmentRoundAclMarketDataQueryAdapter implements MarketDataQueryPort {

    // USDT 시드·긴급 충전의 원화 환산 시세는 이 거래소의 USDT 마켓 현재가에서 읽는다.
    private static final String USDT_KRW_RATE_SOURCE_EXCHANGE = "BITHUMB";

    private final GetPriceChangeRateUseCase getPriceChangeRateUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;

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

    @Override
    public KrwConversionRate getKrwConversionRate(Long exchangeId) {
        return findExchangeDetailUseCase
                .findExchangeDetail(exchangeId)
                .map(this::toKrwConversionRate)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private SeedFundingSpec toSeedFundingSpec(ExchangeDetailResult detail) {
        return new SeedFundingSpec(
                detail.baseCurrencyCoinId(),
                SeedAmountPolicy.forDomestic(detail.domestic()),
                toKrwConversionRate(detail));
    }

    private KrwConversionRate toKrwConversionRate(ExchangeDetailResult detail) {
        if (detail.domestic()) {
            return KrwConversionRate.identity();
        }
        return new KrwConversionRate(usdtKrwPrice(detail.baseCurrencyCoinId()));
    }

    // 시세를 못 찾으면 0 을 담는다. 환산이 실제로 필요한 시점(금액 > 0)에 도메인이 거른다.
    private BigDecimal usdtKrwPrice(Long usdtCoinId) {
        return findExchangeDetailUseCase
                .findExchangeIdByName(USDT_KRW_RATE_SOURCE_EXCHANGE)
                .flatMap(sourceExchangeId ->
                        findExchangeCoinMappingUseCase
                                .findExchangeCoinMappings(sourceExchangeId, List.of(usdtCoinId))
                                .stream()
                                .findFirst())
                .map(mapping -> getLivePriceUseCase.getCurrentPrice(mapping.exchangeCoinId()))
                .orElse(BigDecimal.ZERO);
    }
}
