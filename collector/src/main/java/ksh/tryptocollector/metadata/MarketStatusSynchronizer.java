package ksh.tryptocollector.metadata;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ksh.tryptocollector.distribute.rabbitmq.MarketStatusPublisher;
import ksh.tryptocollector.ha.LeaderElection;
import ksh.tryptocollector.ingest.binance.BinanceRestClient;
import ksh.tryptocollector.ingest.binance.BinanceTickerResponse;
import ksh.tryptocollector.ingest.bithumb.BithumbRestClient;
import ksh.tryptocollector.ingest.upbit.UpbitRestClient;
import ksh.tryptocollector.model.Exchange;
import ksh.tryptocollector.model.MarketInfo;
import ksh.tryptocollector.model.MarketStatusChanged;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketStatusSynchronizer {

    private static final String DOMESTIC_MARKET_PREFIX = "KRW-";
    private static final String BINANCE_QUOTE = "USDT";

    private final LeaderElection leaderElection;
    private final MarketInfoCache marketInfoCache;
    private final MarketMetadataRedisRepository marketMetadataRedisRepository;
    private final MarketStatusPublisher marketStatusPublisher;
    private final UpbitRestClient upbitRestClient;
    private final BithumbRestClient bithumbRestClient;
    private final BinanceRestClient binanceRestClient;

    private final Map<Exchange, Set<String>> tradingBaseline = new EnumMap<>(Exchange.class);
    private boolean wasLeader = false;

    @Scheduled(fixedDelayString = "${market-status.sync-interval-ms:180000}")
    public void sync() {
        if (!leaderElection.isLeader()) {
            wasLeader = false;
            return;
        }
        if (!wasLeader) {
            tradingBaseline.clear();
            wasLeader = true;
        }
        for (Exchange exchange : Exchange.values()) {
            syncExchangeSafely(exchange);
        }
    }

    private void syncExchangeSafely(Exchange exchange) {
        try {
            syncExchange(exchange);
        } catch (Exception e) {
            log.warn("{} 상장 상태 동기화 실패, 이번 회차 무변경: {}", exchange, e.getMessage(), e);
        }
    }

    private void syncExchange(Exchange exchange) {
        Map<String, MarketInfo> currentMarkets = fetchTradingMarkets(exchange);
        if (currentMarkets.isEmpty()) {
            return;
        }
        Set<String> current = currentMarkets.keySet();
        Set<String> previous = tradingBaseline.get(exchange);
        if (previous == null) {
            tradingBaseline.put(exchange, new HashSet<>(current));
            return;
        }
        boolean changed = registerNewlyTrading(exchange, current, previous, currentMarkets);
        changed |= suspendNoLongerTrading(exchange, current, previous);
        if (changed) {
            marketMetadataRedisRepository.save(exchange, marketInfoCache.getMarketInfos(exchange));
        }
        tradingBaseline.put(exchange, new HashSet<>(current));
    }

    private boolean registerNewlyTrading(
            Exchange exchange, Set<String> current, Set<String> previous, Map<String, MarketInfo> markets) {
        boolean changed = false;
        for (String code : current) {
            if (previous.contains(code)) {
                continue;
            }
            MarketInfo info = markets.get(code);
            marketInfoCache.put(exchange, code, info);
            marketStatusPublisher.publish(MarketStatusChanged.trading(exchange, info));
            log.info("{} 신규 거래중 감지: {}", exchange, code);
            changed = true;
        }
        return changed;
    }

    private boolean suspendNoLongerTrading(Exchange exchange, Set<String> current, Set<String> previous) {
        boolean changed = false;
        for (String code : previous) {
            if (current.contains(code)) {
                continue;
            }
            MarketInfo info = marketInfoCache.find(exchange, code).orElse(null);
            if (info == null) {
                continue;
            }
            marketInfoCache.remove(exchange, code);
            marketStatusPublisher.publish(MarketStatusChanged.suspended(exchange, info));
            log.info("{} 거래지원 종료 감지: {}", exchange, code);
            changed = true;
        }
        return changed;
    }

    private Map<String, MarketInfo> fetchTradingMarkets(Exchange exchange) {
        return switch (exchange) {
            case UPBIT -> domesticTradingMarkets(upbitRestClient.fetchKrwMarkets());
            case BITHUMB -> domesticTradingMarkets(bithumbRestClient.fetchKrwMarkets());
            case BINANCE -> binanceTradingMarkets();
        };
    }

    private Map<String, MarketInfo> domesticTradingMarkets(List<MarketInfo> infos) {
        Map<String, MarketInfo> markets = new HashMap<>();
        for (MarketInfo info : infos) {
            markets.put(DOMESTIC_MARKET_PREFIX + info.base(), info);
        }
        return markets;
    }

    private Map<String, MarketInfo> binanceTradingMarkets() {
        Map<String, MarketInfo> markets = new HashMap<>();
        for (BinanceTickerResponse ticker : binanceRestClient.fetchTradingUsdtTickers()) {
            String base = ticker.symbol().replace(BINANCE_QUOTE, "");
            markets.put(ticker.symbol(), new MarketInfo(base, BINANCE_QUOTE, base + "/" + BINANCE_QUOTE, base));
        }
        return markets;
    }
}
