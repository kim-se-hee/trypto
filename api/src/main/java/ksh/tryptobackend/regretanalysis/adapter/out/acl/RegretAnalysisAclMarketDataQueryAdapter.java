package ksh.tryptobackend.regretanalysis.adapter.out.acl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindBtcDailyPricesUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinSymbolsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.BtcDailyPriceResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.regretanalysis.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrice;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrices;
import ksh.tryptobackend.regretanalysis.domain.vo.CurrentPrices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegretAnalysisAclMarketDataQueryAdapter implements MarketDataQueryPort {

    private static final String DOMESTIC_CURRENCY = "KRW";
    private static final String OVERSEAS_CURRENCY = "USDT";

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindCoinSymbolsUseCase findCoinSymbolsUseCase;
    private final FindBtcDailyPricesUseCase findBtcDailyPricesUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;

    @Override
    public AnalysisExchange getExchange(Long exchangeId) {
        ExchangeDetailResult result = findExchangeDetailUseCase
                .findExchangeDetail(exchangeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        return toAnalysisExchange(exchangeId, result);
    }

    @Override
    public Map<Long, String> findCoinSymbols(Set<Long> coinIds) {
        return findCoinSymbolsUseCase.findSymbolsByIds(coinIds);
    }

    @Override
    public BtcDailyPrices findBtcDailyPrices(LocalDate startDate, LocalDate endDate, String currency) {
        List<BtcDailyPrice> prices = findBtcDailyPricesUseCase.findBtcDailyPrices(startDate, endDate, currency).stream()
                .map(this::toBtcDailyPrice)
                .toList();
        return BtcDailyPrices.of(prices);
    }

    @Override
    public CurrentPrices findCurrentPrices(Set<Long> exchangeCoinIds) {
        Map<Long, BigDecimal> priceByExchangeCoinId =
                exchangeCoinIds.stream().collect(Collectors.toMap(id -> id, getLivePriceUseCase::getCurrentPrice));
        return new CurrentPrices(priceByExchangeCoinId);
    }

    private AnalysisExchange toAnalysisExchange(Long exchangeId, ExchangeDetailResult result) {
        String currency = result.domestic() ? DOMESTIC_CURRENCY : OVERSEAS_CURRENCY;
        return new AnalysisExchange(exchangeId, result.name(), currency);
    }

    private BtcDailyPrice toBtcDailyPrice(BtcDailyPriceResult result) {
        return new BtcDailyPrice(result.date(), result.closePrice());
    }
}
