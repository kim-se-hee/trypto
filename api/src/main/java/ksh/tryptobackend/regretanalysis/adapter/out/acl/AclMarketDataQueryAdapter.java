package ksh.tryptobackend.regretanalysis.adapter.out.acl;

import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinSymbolsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.regretanalysis.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("regretanalysisAclMarketDataQueryAdapter")
@RequiredArgsConstructor
public class AclMarketDataQueryAdapter implements MarketDataQueryPort {

    private static final String DOMESTIC_CURRENCY = "KRW";
    private static final String OVERSEAS_CURRENCY = "USDT";

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindCoinSymbolsUseCase findCoinSymbolsUseCase;

    @Override
    public AnalysisExchange getExchange(Long exchangeId) {
        ExchangeDetailResult result =
                findExchangeDetailUseCase
                        .findExchangeDetail(exchangeId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        return toAnalysisExchange(exchangeId, result);
    }

    @Override
    public Map<Long, String> findCoinSymbols(Set<Long> coinIds) {
        return findCoinSymbolsUseCase.findSymbolsByIds(coinIds);
    }

    private AnalysisExchange toAnalysisExchange(Long exchangeId, ExchangeDetailResult result) {
        String currency = result.domestic() ? DOMESTIC_CURRENCY : OVERSEAS_CURRENCY;
        return new AnalysisExchange(exchangeId, result.name(), currency);
    }
}
