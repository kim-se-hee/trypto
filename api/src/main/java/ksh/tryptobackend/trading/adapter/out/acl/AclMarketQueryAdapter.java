package ksh.tryptobackend.trading.adapter.out.acl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.domain.vo.ExchangeInfo;
import ksh.tryptobackend.trading.domain.vo.MarketIdentifier;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.OrderAmountPolicy;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AclMarketQueryAdapter implements MarketQueryPort {

    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindCoinInfoUseCase findCoinInfoUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;

    @Override
    public MarketInfo findByExchangeCoinId(Long exchangeCoinId) {
        ExchangeCoinMappingResult mapping = getMapping(exchangeCoinId);
        ExchangeDetailResult detail = getDetail(mapping.exchangeId());
        BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(exchangeCoinId);

        return new MarketInfo(
                new TradingPair(mapping.coinId(), detail.baseCurrencyCoinId()),
                toExchangeInfo(detail),
                Price.of(currentPrice));
    }

    @Override
    public TradingPair findTradingPair(Long exchangeCoinId) {
        ExchangeCoinMappingResult mapping = getMapping(exchangeCoinId);
        ExchangeDetailResult detail = getDetail(mapping.exchangeId());
        return new TradingPair(mapping.coinId(), detail.baseCurrencyCoinId());
    }

    @Override
    public MarketIdentifier findMarketIdentifier(Long exchangeCoinId) {
        ExchangeCoinMappingResult mapping = getMapping(exchangeCoinId);
        ExchangeDetailResult detail = getDetail(mapping.exchangeId());
        return resolveMarketIdentifier(detail, mapping.coinId());
    }

    private ExchangeCoinMappingResult getMapping(Long exchangeCoinId) {
        return findExchangeCoinMappingUseCase
                .findById(exchangeCoinId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private ExchangeDetailResult getDetail(Long exchangeId) {
        return findExchangeDetailUseCase
                .findExchangeDetail(exchangeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private ExchangeInfo toExchangeInfo(ExchangeDetailResult detail) {
        OrderAmountPolicy policy =
                detail.domestic() ? OrderAmountPolicy.DOMESTIC : OrderAmountPolicy.OVERSEAS;
        return new ExchangeInfo(detail.feeRate(), policy.getMinAmount(), policy.getMaxAmount());
    }

    private MarketIdentifier resolveMarketIdentifier(ExchangeDetailResult detail, Long coinId) {
        Map<Long, CoinInfoResult> coinInfo =
                findCoinInfoUseCase.findByIds(Set.of(coinId, detail.baseCurrencyCoinId()));
        CoinInfoResult coin = requireCoinInfo(coinInfo, coinId);
        CoinInfoResult baseCoin = requireCoinInfo(coinInfo, detail.baseCurrencyCoinId());
        return MarketIdentifier.of(detail.name(), coin.symbol(), baseCoin.symbol());
    }

    private CoinInfoResult requireCoinInfo(Map<Long, CoinInfoResult> coinInfo, Long coinId) {
        CoinInfoResult result = coinInfo.get(coinId);
        if (result == null) {
            throw new CustomException(ErrorCode.COIN_NOT_FOUND);
        }
        return result;
    }
}
