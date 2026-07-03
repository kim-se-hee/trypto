package ksh.tryptobackend.trading.application.service;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.trading.application.port.in.GetOrderAvailabilityUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.GetOrderAvailabilityQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        ExchangeCoinMappingResult mapping = getExchangeCoinMapping(query.exchangeCoinId());

        BigDecimal available = getAvailableBalance(query.walletId(), query.side(), mapping);
        BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(query.exchangeCoinId());

        return new OrderAvailabilityResult(available, currentPrice);
    }

    private ExchangeCoinMappingResult getExchangeCoinMapping(Long exchangeCoinId) {
        return findExchangeCoinMappingUseCase
                .findById(exchangeCoinId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private Long getBaseCurrencyCoinId(Long exchangeId) {
        return findExchangeDetailUseCase
                .findExchangeDetail(exchangeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND))
                .baseCurrencyCoinId();
    }

    private BigDecimal getAvailableBalance(
            Long walletId, Side side, ExchangeCoinMappingResult mapping) {
        Long targetCoinId =
                side == Side.BUY ? getBaseCurrencyCoinId(mapping.exchangeId()) : mapping.coinId();
        return getAvailableBalanceUseCase.getAvailableBalance(walletId, targetCoinId);
    }
}
