package ksh.tryptobackend.acceptance.hook;

import io.cucumber.java.Before;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import ksh.tryptobackend.acceptance.mock.MockBtcPriceHistoryAdapter;
import ksh.tryptobackend.acceptance.mock.MockCandleAdapter;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.acceptance.mock.MockPriceChangeRateAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@RequiredArgsConstructor
public class DatabaseCleanupHook {

    // shedlock 만 청소·재시드 대상에서 제외한다. coin/exchange_market/exchange_coin/exchange_coin_chain/
    // withdrawal_fee 는 TRUNCATE 후 seed-data.sql 로 재적재되므로 시나리오가 추가한 잔재(예: coin-chain 의
    // 102~104 exchange_coin) 가 다음 시나리오로 누설되지 않는다.
    private static final Set<String> CLEANUP_EXCLUDED_TABLES = Set.of("shedlock");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final MockLivePriceAdapter mockLivePriceAdapter;
    private final MockHoldingAdapter mockHoldingAdapter;
    private final MockPriceChangeRateAdapter mockPriceChangeRateAdapter;
    private final MockCandleAdapter mockCandleAdapter;
    private final MockBtcPriceHistoryAdapter mockBtcPriceHistoryAdapter;

    @Before
    public void cleanUp() throws Exception {
        List<String> tables =
                jdbcTemplate.queryForList(
                        "SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA ="
                                + " DATABASE()",
                        String.class);

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

        mockLivePriceAdapter.clear();
        mockHoldingAdapter.clear();
        mockPriceChangeRateAdapter.clear();
        mockCandleAdapter.clear();
        mockBtcPriceHistoryAdapter.clear();
    }
}
