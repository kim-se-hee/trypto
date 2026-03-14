package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.CoinInfo;
import ksh.tryptobackend.marketdata.domain.vo.CoinSymbols;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CoinQueryAdapter implements CoinQueryPort {

    private final CoinJpaRepository coinJpaRepository;

    @Override
    public CoinSymbols findSymbolsByIds(Set<Long> coinIds) {
        if (coinIds.isEmpty()) {
            return new CoinSymbols(Map.of());
        }
        Map<Long, String> symbolMap = coinJpaRepository.findByIdIn(coinIds).stream()
            .collect(Collectors.toMap(CoinJpaEntity::getId, CoinJpaEntity::getSymbol));
        return new CoinSymbols(symbolMap);
    }

    @Override
    public List<CoinInfo> findByIds(Set<Long> coinIds) {
        if (coinIds.isEmpty()) {
            return List.of();
        }
        return coinJpaRepository.findByIdIn(coinIds).stream()
            .map(entity -> new CoinInfo(entity.getId(), entity.getSymbol(), entity.getName()))
            .toList();
    }

}
