package ksh.tryptobackend.portfolio.adapter.out.acl;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.portfolio.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.CoinMetadata;
import ksh.tryptobackend.portfolio.domain.vo.CoinMetadataMap;
import ksh.tryptobackend.portfolio.domain.vo.ExchangeSnapshot;
import ksh.tryptobackend.portfolio.domain.vo.KrwConversionRate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("portfolioAclMarketDataQueryAdapter")
@RequiredArgsConstructor
public class AclMarketDataQueryAdapter implements MarketDataQueryPort {

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindCoinInfoUseCase findCoinInfoUseCase;

    @Override
    public Long getBaseCurrencyCoinId(Long exchangeId) {
        return findExchangeDetailUseCase
                .findExchangeDetail(exchangeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND))
                .baseCurrencyCoinId();
    }

    @Override
    public ExchangeSnapshot getExchangeSnapshot(Long exchangeId) {
        ExchangeDetailResult detail =
                findExchangeDetailUseCase
                        .findExchangeDetail(exchangeId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        KrwConversionRate conversionRate =
                detail.domestic() ? KrwConversionRate.DOMESTIC : KrwConversionRate.OVERSEAS;
        return new ExchangeSnapshot(exchangeId, detail.baseCurrencyCoinId(), conversionRate);
    }

    @Override
    public CoinMetadataMap findCoinMetadata(Set<Long> coinIds) {
        Map<Long, CoinMetadata> metadata =
                findCoinInfoUseCase.findByIds(coinIds).entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey, entry -> toMetadata(entry.getValue())));
        return new CoinMetadataMap(metadata);
    }

    private CoinMetadata toMetadata(CoinInfoResult coinInfo) {
        return new CoinMetadata(coinInfo.symbol(), coinInfo.name());
    }
}
