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
import ksh.tryptobackend.marketdata.application.port.out.PriceChangeRateQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class PriceChangeRateQueryAdapter implements PriceChangeRateQueryPort {

    private static final String TICKER_KEY_PREFIX = "ticker:";

    private final StringRedisTemplate redisTemplate;
    private final ExchangeCoinJpaRepository exchangeCoinRepository;
    private final ExchangeJpaRepository exchangeRepository;
    private final CoinJpaRepository coinRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentMap<Long, String> redisKeyCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getChangeRate(Long exchangeCoinId) {
        String redisKey = redisKeyCache.computeIfAbsent(exchangeCoinId, this::buildRedisKey);
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            return BigDecimal.ZERO;
        }
        return parseChangeRate(json);
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

    private BigDecimal parseChangeRate(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode changeRateNode = node.get("change_rate");
            if (changeRateNode == null) {
                return BigDecimal.ZERO;
            }
            return changeRateNode.decimalValue();
        } catch (JacksonException e) {
            return BigDecimal.ZERO;
        }
    }
}
