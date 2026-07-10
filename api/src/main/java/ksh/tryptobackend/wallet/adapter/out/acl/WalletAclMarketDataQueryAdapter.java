package ksh.tryptobackend.wallet.adapter.out.acl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinSymbolsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.wallet.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.wallet.domain.vo.BaseCurrency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletAclMarketDataQueryAdapter implements MarketDataQueryPort {

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindCoinInfoUseCase findCoinInfoUseCase;
    private final FindCoinSymbolsUseCase findCoinSymbolsUseCase;
    private final FindExchangeCoinsUseCase findExchangeCoinsUseCase;

    @Override
    public BaseCurrency getBaseCurrency(Long exchangeId) {
        Long coinId = getBaseCurrencyCoinId(exchangeId);
        return new BaseCurrency(coinId, resolveSymbol(coinId));
    }

    @Override
    public Map<Long, String> findCoinSymbols(Set<Long> coinIds) {
        return findCoinSymbolsUseCase.findSymbolsByIds(coinIds);
    }

    @Override
    public List<Long> findCoinIdsByExchange(Long exchangeId) {
        return findExchangeCoinsUseCase.findByExchangeId(exchangeId).stream()
                .map(ExchangeCoinListResult::coinId)
                .toList();
    }

    private Long getBaseCurrencyCoinId(Long exchangeId) {
        return findExchangeDetailUseCase
                .findExchangeDetail(exchangeId)
                .map(ExchangeDetailResult::baseCurrencyCoinId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private String resolveSymbol(Long coinId) {
        CoinInfoResult coinInfo = findCoinInfoUseCase.findByIds(Set.of(coinId)).get(coinId);
        if (coinInfo == null) {
            throw new CustomException(ErrorCode.COIN_NOT_FOUND);
        }
        return coinInfo.symbol();
    }
}
