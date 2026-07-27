package ksh.tryptobackend.acceptance.hook;

import io.cucumber.java.Before;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import ksh.tryptobackend.acceptance.mock.MockBtcPriceHistoryAdapter;
import ksh.tryptobackend.acceptance.mock.MockCandleAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@RequiredArgsConstructor
public class DatabaseCleanupHook {

    // shedlock 만 청소·재시드 대상에서 제외한다. coin/exchange_market/exchange_coin 는
    // TRUNCATE 후 seed-data.sql 로 재적재되므로 시나리오가 추가한 잔재가 다음 시나리오로 누설되지 않는다.
    private static final Set<String> CLEANUP_EXCLUDED_TABLES = Set.of("shedlock");

    // 시세는 실물 Redis 를 쓰므로 시나리오가 넣은 티커가 다음 시나리오로 새지 않게 지운다.
    // FLUSHDB 는 티커 외 용도의 키까지 날리므로 쓰지 않는다.
    private static final String TICKER_KEY_PATTERN = "ticker:*";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final MockLivePriceAdapter mockLivePriceAdapter;
    private final MockCandleAdapter mockCandleAdapter;
    private final MockBtcPriceHistoryAdapter mockBtcPriceHistoryAdapter;

    @Before
    public void cleanUp() throws Exception {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA =" + " DATABASE()", String.class);

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tables) {
            if (!CLEANUP_EXCLUDED_TABLES.contains(table.toLowerCase())) {
                jdbcTemplate.execute("TRUNCATE TABLE " + table);
            }
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        try (var conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/seed-data.sql"));
        }

        Set<String> tickerKeys = redisTemplate.keys(TICKER_KEY_PATTERN);
        if (!tickerKeys.isEmpty()) {
            redisTemplate.delete(tickerKeys);
        }

        mockLivePriceAdapter.clear();
        mockCandleAdapter.clear();
        mockBtcPriceHistoryAdapter.clear();
    }
}
