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
            Coin coin = coinQueryPort
                    .findBySymbol(command.baseSymbol())
                    .orElseThrow(() -> new CustomException(ErrorCode.COIN_NOT_FOUND));
            ExchangeCoin suspended = exchangeCoinCommandPort
                    .suspend(exchangeId, coin.coinId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
            marketStatusNotificationPort.broadcast(suspended.toNotification(coin.symbol()));
            domainEventPublisher.publish(new MarketSuspendedEvent(suspended.exchangeCoinId()));
            log.info("거래지원 종료 반영: exchangeId={}, coin={}", exchangeId, command.baseSymbol());
        } else {
            Coin coin = coinCommandPort.save(command.baseSymbol(), command.displayName());
            ExchangeCoin registered =
                    exchangeCoinCommandPort.register(exchangeId, coin.coinId(), command.displayName());
            marketStatusNotificationPort.broadcast(registered.toNotification(coin.symbol()));
            log.info("거래중 반영: exchangeId={}, coin={}", exchangeId, command.baseSymbol());
        }
    }
}
