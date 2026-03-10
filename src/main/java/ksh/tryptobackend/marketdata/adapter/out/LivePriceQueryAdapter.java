package ksh.tryptobackend.marketdata.adapter.out;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.LivePriceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class LivePriceQueryAdapter implements LivePriceQueryPort {

    private static final String TICKER_KEY_PREFIX = "ticker:";

    private final StringRedisTemplate redisTemplate;
    private final ExchangeCoinJpaRepository exchangeCoinRepository;
    private final ExchangeJpaRepository exchangeRepository;
    private final CoinJpaRepository coinRepository;
    private final ObjectMapper objectMapper;

    @Override
    public BigDecimal getCurrentPrice(Long exchangeCoinId) {
        String redisKey = buildRedisKey(exchangeCoinId);
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            throw new CustomException(ErrorCode.PRICE_NOT_AVAILABLE);
        }
        return parseLastPrice(json);
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
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private BigDecimal parseLastPrice(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.get("last_price").decimalValue();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.PRICE_NOT_AVAILABLE);
        }
    }
}
