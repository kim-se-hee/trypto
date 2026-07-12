package ksh.tryptobackend.trading.application.service;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.application.port.in.GetOrderAvailabilityUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.GetOrderAvailabilityQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import ksh.tryptobackend.trading.domain.service.WalletOwnershipVerifier;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOrderAvailabilityService implements GetOrderAvailabilityUseCase {

    private final MarketQueryPort marketQueryPort;
    private final WalletQueryPort walletQueryPort;
    private final WalletOwnershipVerifier walletOwnershipVerifier;

    @Override
    @Transactional(readOnly = true)
    public OrderAvailabilityResult getAvailability(GetOrderAvailabilityQuery query) {
        walletOwnershipVerifier.verify(query.walletId(), query.requesterId());

        TradingPair tradingPair = marketQueryPort.getTradingPair(query.exchangeCoinId());

        BigDecimal available = getAvailableBalance(query.walletId(), query.side(), tradingPair);
        BigDecimal currentPrice =
                marketQueryPort.getCurrentPrice(query.exchangeCoinId()).value();

        return new OrderAvailabilityResult(available, currentPrice);
    }

    private BigDecimal getAvailableBalance(Long walletId, Side side, TradingPair tradingPair) {
        Long targetCoinId = side == Side.BUY ? tradingPair.quoteCoinId() : tradingPair.tradedCoinId();
        return walletQueryPort.getAvailableBalance(walletId, targetCoinId);
    }
}
