package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.GetOrderAvailabilityUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.GetOrderAvailabilityQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort.ExchangeCoinData;
import ksh.tryptobackend.trading.application.port.out.ExchangePort;
import ksh.tryptobackend.trading.application.port.out.ExchangePort.ExchangeData;
import ksh.tryptobackend.trading.application.port.out.LivePricePort;
import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetOrderAvailabilityService implements GetOrderAvailabilityUseCase {

    private final WalletBalancePort walletBalancePort;
    private final LivePricePort livePricePort;
    private final ExchangePort exchangePort;
    private final ExchangeCoinPort exchangeCoinPort;

    @Override
    @Transactional(readOnly = true)
    public OrderAvailabilityResult getAvailability(GetOrderAvailabilityQuery query) {
        ExchangeCoinData exchangeCoin = exchangeCoinPort.findById(query.exchangeCoinId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        ExchangeData exchange = exchangePort.findById(exchangeCoin.exchangeId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        BigDecimal available;
        if (query.side() == Side.BUY) {
            available = walletBalancePort.getAvailableBalance(query.walletId(), exchange.baseCurrencyCoinId());
        } else {
            available = walletBalancePort.getAvailableBalance(query.walletId(), exchangeCoin.coinId());
        }

        BigDecimal currentPrice = livePricePort.getCurrentPrice(query.exchangeCoinId());

        return new OrderAvailabilityResult(available, currentPrice);
    }
}
