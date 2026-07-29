package ksh.tryptoengine.matching;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeCoinResolver {

    private final JdbcTemplate jdbc;
    private final Map<String, Long> cache = new HashMap<>();
    private final Set<String> reportedMisses = new HashSet<>();

    @PostConstruct
    void preload() {
        jdbc.query(
                "SELECT ec.exchange_coin_id, em.name AS exchange_name, c.symbol "
                        + "FROM exchange_coin ec "
                        + "JOIN exchange_market em ON em.exchange_id = ec.exchange_id "
                        + "JOIN coin c ON c.coin_id = ec.coin_id",
                rs -> {
                    cache.put(
                            key(rs.getString("exchange_name"), rs.getString("symbol")), rs.getLong("exchange_coin_id"));
                });
        log.info("ExchangeCoinResolver loaded {} mappings", cache.size());
    }

    public Long resolve(String exchange, String symbol) {
        Long hit = cache.get(key(exchange, symbol));
        if (hit != null) return hit;
        return lazyLoad(exchange, symbol);
    }

    private Long lazyLoad(String exchange, String symbol) {
        try {
            Long id = jdbc.queryForObject(
                    "SELECT ec.exchange_coin_id FROM exchange_coin ec "
                            + "JOIN exchange_market em ON em.exchange_id = ec.exchange_id "
                            + "JOIN coin c ON c.coin_id = ec.coin_id "
                            + "WHERE em.name=? AND c.symbol=?",
                    Long.class,
                    exchange,
                    symbol);
            cache.put(key(exchange, symbol), id);
            return id;
        } catch (Exception e) {
            reportMiss(exchange, symbol);
            return null;
        }
    }

    private void reportMiss(String exchange, String symbol) {
        if (reportedMisses.add(key(exchange, symbol))) {
            log.warn("exchangeCoin 해석 실패, 해당 종목 체결 판정 불가 exchange={} symbol={}", exchange, symbol);
        }
    }

    private String key(String exchange, String symbol) {
        return exchange + ":" + symbol;
    }
}
