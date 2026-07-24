package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.ApplyMarketStatusChangeUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ApplyMarketStatusChangeCommand;
import ksh.tryptobackend.marketdata.application.port.out.CoinCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Coin;
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
        exchangeCoinCommandPort.register(exchangeId, coin.coinId(), command.displayName());
        log.info("거래중 반영: exchangeId={}, coin={}", exchangeId, command.baseSymbol());
    }

    private void suspendMarket(Long exchangeId, String baseSymbol) {
        coinQueryPort
                .findBySymbol(baseSymbol)
                .flatMap(coin -> exchangeCoinCommandPort.suspend(exchangeId, coin.coinId()))
                .ifPresent(suspended -> log.info("거래지원 종료 반영: exchangeId={}, coin={}", exchangeId, baseSymbol));
    }
}
