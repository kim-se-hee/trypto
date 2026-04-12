package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.trading.application.port.in.CancelOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheCommandPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderCommandPort orderCommandPort;
    private final PendingOrderCacheCommandPort pendingOrderCacheCommandPort;

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;

    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;

    @Override
    @Transactional
    public Order cancelOrder(CancelOrderCommand command) {
        Order order = getOrder(command);

        if (order.isAlreadyCancelled()) {
            return order;
        }

        boolean cancelled = orderCommandPort.cancelOrder(command.orderId());
        if (!cancelled) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }

        order.cancel();
        unlockBalance(order);
        try {
            pendingOrderCacheCommandPort.remove(order.getExchangeCoinId(), order.getId());
        } catch (Exception e) {
            log.error("Redis ZREM 실패: orderId={}", order.getId(), e);
        }

        return order;
    }

    private Order getOrder(CancelOrderCommand command) {
        Order order = orderCommandPort.findById(command.orderId())
            .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.isOwnedBy(command.walletId())) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }

        return order;
    }

    private void unlockBalance(Order order) {
        ExchangeCoinMappingResult mapping = getExchangeCoinMapping(order.getExchangeCoinId());

        if (order.isBuyOrder()) {
            TradingVenue venue = getTradingVenue(mapping.exchangeId());
            manageWalletBalanceUseCase.unlockBalance(order.getWalletId(), venue.baseCurrencyCoinId(), order.getSettlementDebit());
        } else {
            manageWalletBalanceUseCase.unlockBalance(order.getWalletId(), mapping.coinId(), order.getQuantity().value());
        }
    }

    private ExchangeCoinMappingResult getExchangeCoinMapping(Long exchangeCoinId) {
        return findExchangeCoinMappingUseCase.findById(exchangeCoinId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private TradingVenue getTradingVenue(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(detail -> TradingVenue.of(detail.feeRate(), detail.baseCurrencyCoinId(), detail.domestic()))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }
}
