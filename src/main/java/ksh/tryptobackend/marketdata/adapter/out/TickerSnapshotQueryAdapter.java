package ksh.tryptobackend.marketdata.adapter.out;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.TickerSnapshotQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshot;
import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshots;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TickerSnapshotQueryAdapter implements TickerSnapshotQueryPort {

    private static final String TICKER_KEY_PREFIX = "ticker:";

    private final StringRedisTemplate redisTemplate;
    private final ExchangeCoinJpaRepository exchangeCoinRepository;
    private final ExchangeJpaRepository exchangeRepository;
    private final CoinJpaRepository coinRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentMap<Long, String> redisKeyCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public TickerSnapshots findByExchangeCoinIds(Set<Long> exchangeCoinIds) {
        if (exchangeCoinIds.isEmpty()) {
            return new TickerSnapshots(Map.of());
        }

        buildRedisKeysInBatch(exchangeCoinIds);

        List<Long> ids = new ArrayList<>(exchangeCoinIds);
        List<String> keys = ids.stream()
            .map(redisKeyCache::get)
            .toList();

        List<String> jsons = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, TickerSnapshot> result = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            String json = jsons != null ? jsons.get(i) : null;
            result.put(ids.get(i), parseTickerSnapshot(json));
        }
        return new TickerSnapshots(result);
    }

    private void buildRedisKeysInBatch(Set<Long> exchangeCoinIds) {
        Set<Long> uncachedIds = exchangeCoinIds.stream()
            .filter(id -> !redisKeyCache.containsKey(id))
            .collect(Collectors.toSet());

        if (uncachedIds.isEmpty()) {
            return;
        }

        List<ExchangeCoinJpaEntity> exchangeCoins = exchangeCoinRepository.findByIdIn(uncachedIds);

        Set<Long> exchangeIds = exchangeCoins.stream()
            .map(ExchangeCoinJpaEntity::getExchangeId)
            .collect(Collectors.toSet());
        Map<Long, ExchangeJpaEntity> exchangeMap = exchangeRepository.findAllById(exchangeIds).stream()
            .collect(Collectors.toMap(ExchangeJpaEntity::getId, e -> e));

        Set<Long> coinIds = new HashSet<>();
        exchangeCoins.forEach(ec -> coinIds.add(ec.getCoinId()));
        exchangeMap.values().forEach(ex -> coinIds.add(ex.getBaseCurrencyCoinId()));

        Map<Long, String> symbolMap = coinRepository.findByIdIn(coinIds).stream()
            .collect(Collectors.toMap(CoinJpaEntity::getId, CoinJpaEntity::getSymbol));

        for (ExchangeCoinJpaEntity ec : exchangeCoins) {
            ExchangeJpaEntity exchange = exchangeMap.get(ec.getExchangeId());
            String baseSymbol = symbolMap.get(ec.getCoinId());
            String quoteSymbol = symbolMap.get(exchange.getBaseCurrencyCoinId());
            String key = TICKER_KEY_PREFIX + exchange.getName() + ":" + baseSymbol + "/" + quoteSymbol;
            redisKeyCache.put(ec.getId(), key);
        }
    }

    private TickerSnapshot parseTickerSnapshot(String json) {
        if (json == null) {
            return TickerSnapshot.EMPTY;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            BigDecimal price = decimalOrZero(node.get("lastPrice"));
            BigDecimal changeRate = decimalOrZero(node.get("changeRate"));
            BigDecimal volume = decimalOrZero(node.get("quoteTurnover"));
            return new TickerSnapshot(price, changeRate, volume);
        } catch (JacksonException e) {
            return TickerSnapshot.EMPTY;
        }
    }

    private BigDecimal decimalOrZero(JsonNode node) {
        return node != null ? node.decimalValue() : BigDecimal.ZERO;
    }
}
