package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.TickerSnapshotQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Coin;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshots;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        validateExchangeExists(exchangeId);
        List<ExchangeCoin> exchangeCoins = exchangeCoinQueryPort.findByExchangeId(exchangeId);
        Map<Long, Coin> coinMap = findCoinMap(exchangeCoins);
        TickerSnapshots tickerSnapshots = findTickerSnapshots(exchangeCoins);
        return toResults(exchangeCoins, coinMap, tickerSnapshots);
    }

    private void validateExchangeExists(Long exchangeId) {
        if (!exchangeQueryPort.existsById(exchangeId)) {
            throw new CustomException(ErrorCode.EXCHANGE_NOT_FOUND);
        }
    }

    private Map<Long, Coin> findCoinMap(List<ExchangeCoin> exchangeCoins) {
        Set<Long> coinIds = exchangeCoins.stream()
            .map(ExchangeCoin::coinId)
            .collect(Collectors.toSet());
        return coinQueryPort.findByIds(coinIds).stream()
            .collect(Collectors.toMap(Coin::coinId, coin -> coin));
    }

    private TickerSnapshots findTickerSnapshots(List<ExchangeCoin> exchangeCoins) {
        Set<Long> exchangeCoinIds = exchangeCoins.stream()
            .map(ExchangeCoin::exchangeCoinId)
            .collect(Collectors.toSet());
        return tickerSnapshotQueryPort.findByExchangeCoinIds(exchangeCoinIds);
    }

    private List<ExchangeCoinListResult> toResults(List<ExchangeCoin> exchangeCoins,
                                                    Map<Long, Coin> coinMap,
                                                    TickerSnapshots tickerSnapshots) {
        return exchangeCoins.stream()
            .map(ec -> toResult(ec, coinMap, tickerSnapshots))
            .toList();
    }

    private ExchangeCoinListResult toResult(ExchangeCoin exchangeCoin,
                                             Map<Long, Coin> coinMap,
                                             TickerSnapshots tickerSnapshots) {
        Coin coin = coinMap.get(exchangeCoin.coinId());
        return new ExchangeCoinListResult(
            exchangeCoin.exchangeCoinId(),
            exchangeCoin.coinId(),
            coin.symbol(),
            coin.name(),
            tickerSnapshots.getSnapshot(exchangeCoin.exchangeCoinId())
        );
    }
}
