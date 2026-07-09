package ksh.tryptobackend.marketdata.application.service;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.TickerSnapshotQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoins;
import ksh.tryptobackend.marketdata.domain.vo.CoinSymbols;
import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshots;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindExchangeCoinsService implements FindExchangeCoinsUseCase {

    private final ExchangeQueryPort exchangeQueryPort;
    private final ExchangeCoinQueryPort exchangeCoinQueryPort;
    private final CoinQueryPort coinQueryPort;
    private final TickerSnapshotQueryPort tickerSnapshotQueryPort;

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeCoinListResult> findByExchangeId(Long exchangeId) {
        if (!exchangeQueryPort.existsById(exchangeId)) {
            throw new CustomException(ErrorCode.EXCHANGE_NOT_FOUND);
        }
        ExchangeCoins exchangeCoins = exchangeCoinQueryPort.findByExchangeId(exchangeId);
        CoinSymbols coinSymbols = coinQueryPort.findSymbolsByIds(exchangeCoins.coinIds());
        TickerSnapshots tickerSnapshots =
                tickerSnapshotQueryPort.findByExchangeCoinIds(exchangeCoins.exchangeCoinIds());
        return exchangeCoins.values().stream()
                .map(
                        exchangeCoin ->
                                ExchangeCoinListResult.of(
                                        exchangeCoin,
                                        coinSymbols.getSymbol(exchangeCoin.coinId()),
                                        tickerSnapshots.getSnapshot(exchangeCoin.exchangeCoinId())))
                .toList();
    }
}
