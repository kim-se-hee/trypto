package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindAllExchangeIdsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.WarmupExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheCommandPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarmupExchangeCoinMappingService implements WarmupExchangeCoinMappingUseCase {

    private final ExchangeCoinMappingCacheCommandPort exchangeCoinMappingCacheCommandPort;

    private final FindAllExchangeIdsUseCase findAllExchangeIdsUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindExchangeCoinsUseCase findExchangeCoinsUseCase;
    private final FindCoinInfoUseCase findCoinInfoUseCase;

    @Override
    public void warmup() {
        List<Long> exchangeIds = findAllExchangeIdsUseCase.findAllExchangeIds();
        Map<Long, ExchangeDetailResult> detailMap = loadExchangeDetailMap(exchangeIds);
        Map<Long, String> baseCurrencySymbols = loadBaseCurrencySymbols(detailMap);

        Map<ExchangeSymbolKey, ExchangeCoinMapping> mappings = new HashMap<>();
        for (Map.Entry<Long, ExchangeDetailResult> entry : detailMap.entrySet()) {
            mappings.putAll(buildMappingsForExchange(entry.getKey(), entry.getValue(), baseCurrencySymbols));
        }

        exchangeCoinMappingCacheCommandPort.loadAll(mappings);
        log.info("거래소-코인 매핑 캐시 로딩 완료: {}건", mappings.size());
    }

    private Map<Long, ExchangeDetailResult> loadExchangeDetailMap(List<Long> exchangeIds) {
        Map<Long, ExchangeDetailResult> detailMap = new HashMap<>();
        for (Long exchangeId : exchangeIds) {
            findExchangeDetailUseCase.findExchangeDetail(exchangeId)
                .ifPresent(detail -> detailMap.put(exchangeId, detail));
        }
        return detailMap;
    }

    private Map<Long, String> loadBaseCurrencySymbols(Map<Long, ExchangeDetailResult> detailMap) {
        Set<Long> baseCoinIds = detailMap.values().stream()
            .map(ExchangeDetailResult::baseCurrencyCoinId)
            .collect(Collectors.toSet());
        Map<Long, CoinInfoResult> coinInfoMap = findCoinInfoUseCase.findByIds(baseCoinIds);
        return coinInfoMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().symbol()));
    }

    private Map<ExchangeSymbolKey, ExchangeCoinMapping> buildMappingsForExchange(
            Long exchangeId, ExchangeDetailResult detail, Map<Long, String> baseCurrencySymbols) {
        String baseCurrencySymbol = baseCurrencySymbols.get(detail.baseCurrencyCoinId());
        if (baseCurrencySymbol == null) {
            return Map.of();
        }

        return findExchangeCoinsUseCase.findByExchangeId(exchangeId).stream()
            .collect(Collectors.toMap(
                ec -> ExchangeSymbolKey.of(detail.name(), ec.coinSymbol(), baseCurrencySymbol),
                ec -> new ExchangeCoinMapping(ec.exchangeCoinId(), exchangeId, ec.coinId(), ec.coinSymbol())));
    }
}
