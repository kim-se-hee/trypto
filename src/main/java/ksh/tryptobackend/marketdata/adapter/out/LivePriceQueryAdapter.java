package ksh.tryptobackend.marketdata.adapter.out;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.LivePriceQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.LivePrices;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class LivePriceQueryAdapter implements LivePriceQueryPort {

    private static final String TICKER_KEY_PREFIX = "ticker:";

    private final StringRedisTemplate redisTemplate;
    private final ExchangeCoinJpaRepository exchangeCoinRepository;
    private final ExchangeJpaRepository exchangeRepository;
    private final CoinJpaRepository coinRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentMap<Long, String> redisKeyCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPrice(Long exchangeCoinId) {
        String redisKey = redisKeyCache.computeIfAbsent(exchangeCoinId, this::buildRedisKey);
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            throw new CustomException(ErrorCode.PRICE_NOT_AVAILABLE);
        }
        return parseLastPrice(json);
    }

    @Override
    public LivePrices getCurrentPrices(Set<Long> exchangeCoinIds) {
        List<Long> ids = new ArrayList<>(exchangeCoinIds);
        List<String> keys = ids.stream()
                .map(id -> redisKeyCache.computeIfAbsent(id, this::buildRedisKey))
                .toList();

        List<String> jsons = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, BigDecimal> result = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            String json = jsons.get(i);
            if (json == null) {
                throw new CustomException(ErrorCode.PRICE_NOT_AVAILABLE);
            }
            result.put(ids.get(i), parseLastPrice(json));
        }
        return new LivePrices(result);
    }

    private String buildRedisKey(Long exchangeCoinId) {
        ExchangeCoinJpaEntity exchangeCoin = exchangeCoinRepository.findById(exchangeCoinId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        ExchangeJpaEntity exchange = exchangeRepository.findById(exchangeCoin.getExchangeId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        String baseSymbol = findCoinSymbol(exchangeCoin.getCoinId());
        String quoteSymbol = findCoinSymbol(exchange.getBaseCurrencyCoinId());

        return TICKER_KEY_PREFIX + exchange.getName() + ":" + baseSymbol + "/" + quoteSymbol;
    }

    private String findCoinSymbol(Long coinId) {
        return coinRepository.findById(coinId)
            .map(CoinJpaEntity::getSymbol)
            .orElseThrow(() -> new CustomException(ErrorCode.COIN_NOT_FOUND));
    }

    private BigDecimal parseLastPrice(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode lastPriceNode = node.get("last_price");
            if (lastPriceNode == null) {
                throw new CustomException(ErrorCode.PRICE_NOT_AVAILABLE);
            }
            return lastPriceNode.decimalValue();
        } catch (JacksonException e) {
            throw new CustomException(ErrorCode.PRICE_NOT_AVAILABLE);
        }
    }
}
