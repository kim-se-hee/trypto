package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Coin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindCoinInfoService implements FindCoinInfoUseCase {

    private final CoinQueryPort coinQueryPort;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, CoinInfoResult> findByIds(Set<Long> coinIds) {
        return coinQueryPort.findByIds(coinIds).stream()
            .collect(Collectors.toMap(Coin::coinId, this::toCoinInfoResult));
    }

    private CoinInfoResult toCoinInfoResult(Coin coin) {
        return new CoinInfoResult(coin.symbol(), coin.name());
    }
}
