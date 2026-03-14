package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindCoinSymbolsUseCase;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FindCoinSymbolsService implements FindCoinSymbolsUseCase {

    private final CoinQueryPort coinQueryPort;

    @Override
    public Map<Long, String> findSymbolsByIds(Set<Long> coinIds) {
        return coinQueryPort.findSymbolsByIds(coinIds).toMap();
    }
}
