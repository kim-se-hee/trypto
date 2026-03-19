package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.SyncMarketMetaUseCase;
import ksh.tryptobackend.marketdata.application.port.out.CoinCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeConfigQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.MarketMetaQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Coin;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeConfig;
import ksh.tryptobackend.marketdata.domain.vo.MarketMetaEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncMarketMetaService implements SyncMarketMetaUseCase {

    private final MarketMetaQueryPort marketMetaQueryPort;
    private final ExchangeConfigQueryPort exchangeConfigQueryPort;
    private final CoinCommandPort coinCommandPort;
    private final ExchangeCommandPort exchangeCommandPort;
    private final ExchangeCoinCommandPort exchangeCoinCommandPort;

    @Override
    @Transactional
    public boolean sync() {
        Map<String, List<MarketMetaEntry>> marketMetaMap = marketMetaQueryPort.findAll();
        if (marketMetaMap.isEmpty()) {
            return false;
        }

        List<ExchangeConfig> exchangeConfigs = exchangeConfigQueryPort.findAll();

        Map<String, Long> coinIdBySymbol = syncCoins(marketMetaMap, exchangeConfigs);
        Map<String, Long> exchangeIdByName = syncExchanges(exchangeConfigs, coinIdBySymbol);
        syncExchangeCoins(marketMetaMap, coinIdBySymbol, exchangeIdByName);
        return true;
    }

    private Map<String, Long> syncCoins(Map<String, List<MarketMetaEntry>> marketMetaMap,
                                         List<ExchangeConfig> exchangeConfigs) {
        Map<String, String> symbolToName = resolveSymbolNames(marketMetaMap, exchangeConfigs);

        Map<String, Long> coinIdBySymbol = new HashMap<>();
        for (Map.Entry<String, String> entry : symbolToName.entrySet()) {
            Coin coin = coinCommandPort.save(entry.getKey(), entry.getValue());
            coinIdBySymbol.put(coin.symbol(), coin.coinId());
        }
        log.info("coin 동기화 완료: {}건", coinIdBySymbol.size());
        return coinIdBySymbol;
    }

    private Map<String, String> resolveSymbolNames(Map<String, List<MarketMetaEntry>> marketMetaMap,
                                                    List<ExchangeConfig> exchangeConfigs) {
        Set<String> domesticExchanges = exchangeConfigs.stream()
                .filter(c -> c.marketType().isDomestic())
                .map(ExchangeConfig::name)
                .collect(Collectors.toSet());

        Map<String, String> symbolToName = new LinkedHashMap<>();

        // 해외 거래소 먼저 처리
        marketMetaMap.forEach((exchangeName, entries) -> {
            if (!domesticExchanges.contains(exchangeName)) {
                for (MarketMetaEntry entry : entries) {
                    symbolToName.putIfAbsent(entry.base(), entry.displayName());
                }
            }
        });

        // 국내 거래소로 덮어쓰기 (한국어명 우선)
        marketMetaMap.forEach((exchangeName, entries) -> {
            if (domesticExchanges.contains(exchangeName)) {
                for (MarketMetaEntry entry : entries) {
                    symbolToName.put(entry.base(), entry.displayName());
                }
            }
        });

        // quote 심볼 추가 (KRW, USDT 등)
        marketMetaMap.values().stream()
                .flatMap(List::stream)
                .map(MarketMetaEntry::quote)
                .distinct()
                .forEach(quote -> symbolToName.putIfAbsent(quote, quote));

        return symbolToName;
    }

    private Map<String, Long> syncExchanges(List<ExchangeConfig> exchangeConfigs,
                                             Map<String, Long> coinIdBySymbol) {
        Map<String, Long> exchangeIdByName = new HashMap<>();

        for (ExchangeConfig config : exchangeConfigs) {
            Long baseCurrencyCoinId = coinIdBySymbol.get(config.baseCurrencySymbol());
            if (baseCurrencyCoinId == null) {
                log.error("기축통화 심볼 {}에 해당하는 coin이 없습니다. 거래소 {} 건너뜀",
                        config.baseCurrencySymbol(), config.name());
                continue;
            }

            Exchange exchange = exchangeCommandPort.save(
                    config.name(), config.marketType(), baseCurrencyCoinId, config.feeRate());
            exchangeIdByName.put(exchange.getName(), exchange.getExchangeId());
        }
        log.info("exchange_market 동기화 완료: {}건", exchangeIdByName.size());
        return exchangeIdByName;
    }

    private void syncExchangeCoins(Map<String, List<MarketMetaEntry>> marketMetaMap,
                                   Map<String, Long> coinIdBySymbol,
                                   Map<String, Long> exchangeIdByName) {
        int count = 0;
        for (Map.Entry<String, List<MarketMetaEntry>> entry : marketMetaMap.entrySet()) {
            String exchangeName = entry.getKey();
            Long exchangeId = exchangeIdByName.get(exchangeName);
            if (exchangeId == null) {
                log.warn("거래소 {}가 yml 설정에 없어 exchange_coin 동기화를 건너뜁니다", exchangeName);
                continue;
            }

            for (MarketMetaEntry meta : entry.getValue()) {
                Long coinId = coinIdBySymbol.get(meta.base());
                if (coinId == null) {
                    continue;
                }
                exchangeCoinCommandPort.save(exchangeId, coinId);
                count++;
            }
        }
        log.info("exchange_coin 동기화 완료: {}건", count);
    }
}
