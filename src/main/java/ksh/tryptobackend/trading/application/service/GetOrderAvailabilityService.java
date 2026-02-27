package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.GetOrderAvailabilityUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.GetOrderAvailabilityQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort.ExchangeCoinData;
import ksh.tryptobackend.trading.application.port.out.LivePricePort;
import ksh.tryptobackend.trading.application.port.out.TradingVenuePort;
import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetOrderAvailabilityService implements GetOrderAvailabilityUseCase {

    private final WalletBalancePort walletBalancePort;
    private final LivePricePort livePricePort;
    private final TradingVenuePort tradingVenuePort;
    private final ExchangeCoinPort exchangeCoinPort;

    @Override
    @Transactional(readOnly = true)
    public OrderAvailabilityResult getAvailability(GetOrderAvailabilityQuery query) {
        ExchangeCoinData exchangeCoin = getExchangeCoin(query.exchangeCoinId());
        TradingVenue venue = getTradingVenue(exchangeCoin.exchangeId());

        BigDecimal available = getAvailableBalance(query.walletId(), query.side(), venue, exchangeCoin);
        BigDecimal currentPrice = livePricePort.getCurrentPrice(query.exchangeCoinId());

        return new OrderAvailabilityResult(available, currentPrice);
    }

    private ExchangeCoinData getExchangeCoin(Long exchangeCoinId) {
        return exchangeCoinPort.findById(exchangeCoinId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private TradingVenue getTradingVenue(Long exchangeId) {
        return tradingVenuePort.findByExchangeId(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private BigDecimal getAvailableBalance(Long walletId, Side side,
                                            TradingVenue venue, ExchangeCoinData exchangeCoin) {
        Long targetCoinId = side == Side.BUY
            ? venue.baseCurrencyCoinId()
            : exchangeCoin.coinId();
        return walletBalancePort.getAvailableBalance(walletId, targetCoinId);
    }
}
