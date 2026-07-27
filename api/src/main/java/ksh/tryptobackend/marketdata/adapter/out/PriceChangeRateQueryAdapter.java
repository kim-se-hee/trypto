package ksh.tryptobackend.marketdata.adapter.out;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.adapter.out.persistence.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.persistence.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.persistence.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.persistence.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.persistence.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.persistence.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.PriceChangeRateQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PriceChangeRateQueryAdapter implements PriceChangeRateQueryPort {

    private static final String TICKER_KEY_PREFIX = "ticker:";

    // collector 가 적재하는 changeRate 는 비율(1% = 0.01)이고 투자 원칙 기준값은 퍼센트(1% = 1)다.
    // 단위 환산은 두 표현이 만나는 이 어댑터 경계에서 끝내고, 도메인에는 퍼센트만 넘긴다.
    private static final BigDecimal RATIO_TO_PERCENT = BigDecimal.valueOf(100);

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
        ExchangeCoinJpaEntity exchangeCoin = exchangeCoinRepository
                .findById(exchangeCoinId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        ExchangeJpaEntity exchange = exchangeRepository
                .findById(exchangeCoin.getExchangeId())
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        String baseSymbol = findCoinSymbol(exchangeCoin.getCoinId());
        String quoteSymbol = findCoinSymbol(exchange.getBaseCurrencyCoinId());

        return TICKER_KEY_PREFIX + exchange.getName() + ":" + baseSymbol + "/" + quoteSymbol;
    }

    private String findCoinSymbol(Long coinId) {
        return coinRepository
                .findById(coinId)
                .map(CoinJpaEntity::getSymbol)
                .orElseThrow(() -> new CustomException(ErrorCode.COIN_NOT_FOUND));
    }

    private BigDecimal parseChangeRate(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode changeRateNode = node.get("changeRate");
            if (changeRateNode == null) {
                return BigDecimal.ZERO;
            }
            return changeRateNode.decimalValue().multiply(RATIO_TO_PERCENT);
        } catch (JacksonException e) {
            return BigDecimal.ZERO;
        }
    }
}
