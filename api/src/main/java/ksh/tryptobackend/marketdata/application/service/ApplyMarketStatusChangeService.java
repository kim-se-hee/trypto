package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.common.event.DomainEventPublisher;
import ksh.tryptobackend.common.event.MarketSuspendedEvent;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.ApplyMarketStatusChangeUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ApplyMarketStatusChangeCommand;
import ksh.tryptobackend.marketdata.application.port.out.CoinCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.MarketStatusNotificationPort;
import ksh.tryptobackend.marketdata.domain.model.Coin;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSummary;
import ksh.tryptobackend.marketdata.domain.vo.MarketStatusNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyMarketStatusChangeService implements ApplyMarketStatusChangeUseCase {

    private final ExchangeQueryPort exchangeQueryPort;
    private final CoinQueryPort coinQueryPort;
    private final CoinCommandPort coinCommandPort;
    private final ExchangeCoinCommandPort exchangeCoinCommandPort;
    private final MarketStatusNotificationPort marketStatusNotificationPort;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public void apply(ApplyMarketStatusChangeCommand command) {
        Long exchangeId = exchangeQueryPort
                .findExchangeSummaryByName(command.exchange())
                .map(ExchangeSummary::exchangeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        if (command.status().isSuspended()) {
            suspendMarket(exchangeId, command.baseSymbol());
        } else {
            startTrading(exchangeId, command);
        }
    }

    private void startTrading(Long exchangeId, ApplyMarketStatusChangeCommand command) {
        Coin coin = coinCommandPort.save(command.baseSymbol(), command.displayName());
        ExchangeCoin registered = exchangeCoinCommandPort.register(exchangeId, coin.coinId(), command.displayName());
        marketStatusNotificationPort.broadcast(notificationOf(registered, coin.symbol()));
        log.info("거래중 반영: exchangeId={}, coin={}", exchangeId, command.baseSymbol());
    }

    private void suspendMarket(Long exchangeId, String baseSymbol) {
        Coin coin =
                coinQueryPort.findBySymbol(baseSymbol).orElseThrow(() -> new CustomException(ErrorCode.COIN_NOT_FOUND));
        ExchangeCoin suspended = exchangeCoinCommandPort
                .suspend(exchangeId, coin.coinId())
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
        marketStatusNotificationPort.broadcast(notificationOf(suspended, coin.symbol()));
        domainEventPublisher.publish(new MarketSuspendedEvent(suspended.exchangeCoinId()));
        log.info("거래지원 종료 반영: exchangeId={}, coin={}", exchangeId, baseSymbol);
    }

    private MarketStatusNotification notificationOf(ExchangeCoin exchangeCoin, String symbol) {
        return new MarketStatusNotification(
                exchangeCoin.exchangeId(),
                exchangeCoin.exchangeCoinId(),
                symbol,
                exchangeCoin.displayName(),
                exchangeCoin.status());
    }
}
