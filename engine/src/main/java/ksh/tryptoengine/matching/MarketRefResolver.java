package ksh.tryptoengine.matching;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketRefResolver {

    private final JdbcTemplate jdbc;
    private final Map<Long, MarketRef> cache = new HashMap<>();

    // feeScale: 수수료 내림 절삭 자릿수 — 기축통화 기준 (DOMESTIC=KRW 정수, OVERSEAS=USDT 8자리)
    public record MarketRef(Long coinId, Long baseCoinId, BigDecimal feeRate, int feeScale) {}

    @PostConstruct
    void preload() {
        jdbc.query(
                "SELECT ec.exchange_coin_id, ec.coin_id, em.base_currency_coin_id, em.fee_rate, em.market_type "
                        + "FROM exchange_coin ec "
                        + "JOIN exchange_market em ON em.exchange_id = ec.exchange_id",
                rs -> {
                    cache.put(
                            rs.getLong("exchange_coin_id"),
                            new MarketRef(
                                    rs.getLong("coin_id"),
                                    rs.getLong("base_currency_coin_id"),
                                    rs.getBigDecimal("fee_rate"),
                                    feeScaleOf(rs.getString("market_type"))));
                });
        log.info("MarketRefResolver loaded {} mappings", cache.size());
    }

    public MarketRef resolve(Long exchangeCoinId) {
        MarketRef hit = cache.get(exchangeCoinId);
        if (hit != null) return hit;
        return lazyLoad(exchangeCoinId);
    }

    private MarketRef lazyLoad(Long exchangeCoinId) {
        try {
            MarketRef ref = jdbc.queryForObject(
                    "SELECT ec.coin_id, em.base_currency_coin_id, em.fee_rate, em.market_type "
                            + "FROM exchange_coin ec "
                            + "JOIN exchange_market em ON em.exchange_id = ec.exchange_id "
                            + "WHERE ec.exchange_coin_id = ?",
                    (rs, rowNum) -> new MarketRef(
                            rs.getLong("coin_id"),
                            rs.getLong("base_currency_coin_id"),
                            rs.getBigDecimal("fee_rate"),
                            feeScaleOf(rs.getString("market_type"))),
                    exchangeCoinId);
            cache.put(exchangeCoinId, ref);
            return ref;
        } catch (Exception e) {
            log.error("market ref resolve failed exchangeCoinId={}", exchangeCoinId, e);
            return null;
        }
    }

    private static int feeScaleOf(String marketType) {
        return "DOMESTIC".equals(marketType) ? 0 : 8;
    }
}
