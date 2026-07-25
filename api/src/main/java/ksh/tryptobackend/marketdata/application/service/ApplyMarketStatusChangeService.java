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
            Coin coin = coinQueryPort
                    .findBySymbol(command.baseSymbol())
                    .orElseThrow(() -> new CustomException(ErrorCode.COIN_NOT_FOUND));
            exchangeCoinCommandPort
                    .suspend(exchangeId, coin.coinId(), coin.symbol())
                    .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
            log.info("거래지원 종료 반영: exchangeId={}, coin={}", exchangeId, command.baseSymbol());
        } else {
            Coin coin = coinCommandPort.save(command.baseSymbol(), command.displayName());
            exchangeCoinCommandPort.register(exchangeId, coin.coinId(), command.displayName(), coin.symbol());
            log.info("거래중 반영: exchangeId={}, coin={}", exchangeId, command.baseSymbol());
        }
    }
}
