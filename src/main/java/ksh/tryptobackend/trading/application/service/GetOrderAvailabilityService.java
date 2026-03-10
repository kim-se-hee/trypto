package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.trading.application.port.in.GetOrderAvailabilityUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.GetOrderAvailabilityQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.trading.domain.vo.ListedCoinRef;
import ksh.tryptobackend.trading.domain.vo.OrderAmountPolicy;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetOrderAvailabilityService implements GetOrderAvailabilityUseCase {

    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;

    @Override
    @Transactional(readOnly = true)
    public OrderAvailabilityResult getAvailability(GetOrderAvailabilityQuery query) {
        ListedCoinRef listedCoin = getListedCoin(query.exchangeCoinId());
        TradingVenue venue = getTradingVenue(listedCoin.exchangeId());

        BigDecimal available = getAvailableBalance(query.walletId(), query.side(), venue, listedCoin);
        BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(query.exchangeCoinId());

        return new OrderAvailabilityResult(available, currentPrice);
    }

    private ListedCoinRef getListedCoin(Long exchangeCoinId) {
        return findExchangeCoinMappingUseCase.findById(exchangeCoinId)
            .map(m -> new ListedCoinRef(m.exchangeCoinId(), m.exchangeId(), m.coinId()))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private TradingVenue getTradingVenue(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(detail -> new TradingVenue(
                detail.feeRate(),
                detail.baseCurrencyCoinId(),
                detail.domestic() ? OrderAmountPolicy.DOMESTIC : OrderAmountPolicy.OVERSEAS))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private BigDecimal getAvailableBalance(Long walletId, Side side,
                                            TradingVenue venue, ListedCoinRef listedCoin) {
        Long targetCoinId = side == Side.BUY
            ? venue.baseCurrencyCoinId()
            : listedCoin.coinId();
        return getAvailableBalanceUseCase.getAvailableBalance(walletId, targetCoinId);
    }
}
