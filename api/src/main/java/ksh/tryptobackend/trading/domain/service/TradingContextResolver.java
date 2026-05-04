package ksh.tryptobackend.trading.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.vo.MarketIdentifier;
import ksh.tryptobackend.trading.domain.vo.OrderMode;
import ksh.tryptobackend.trading.domain.vo.TradingContext;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradingContextResolver {

    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindCoinInfoUseCase findCoinInfoUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;

    private final GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;

    private final Clock clock;

    public TradingContext resolve(PlaceOrderCommand cmd) {
        var mapping =
                findExchangeCoinMappingUseCase
                        .findById(cmd.exchangeCoinId())
                        .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        ExchangeDetailResult detail =
                findExchangeDetailUseCase
                        .findExchangeDetail(mapping.exchangeId())
                        .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        TradingVenue venue =
                TradingVenue.of(detail.feeRate(), detail.baseCurrencyCoinId(), detail.domestic());

        MarketIdentifier marketIdentifier = resolveMarketIdentifier(detail, mapping.coinId());

        BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(cmd.exchangeCoinId());
        OrderMode mode = OrderMode.of(cmd.orderType(), cmd.side());
        Long userId = getWalletOwnerIdUseCase.getWalletOwnerId(cmd.walletId());

        return new TradingContext(
                userId,
                mapping.coinId(),
                venue,
                mode,
                currentPrice,
                LocalDateTime.now(clock),
                marketIdentifier);
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
