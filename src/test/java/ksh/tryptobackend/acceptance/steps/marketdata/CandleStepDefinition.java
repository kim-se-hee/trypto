package ksh.tryptobackend.acceptance.steps.marketdata;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import ksh.tryptobackend.acceptance.mock.MockCandleAdapter;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class CandleStepDefinition {

    private final CommonApiClient apiClient;
    private final MockCandleAdapter mockCandleAdapter;
    private final JdbcTemplate jdbcTemplate;

    public CandleStepDefinition(CommonApiClient apiClient, MockCandleAdapter mockCandleAdapter,
                                JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.mockCandleAdapter = mockCandleAdapter;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        mockCandleAdapter.clear();
    }

    @Given("캔들 데이터가 존재한다")
    public void 캔들_데이터가_존재한다() {
        insertExchangeIfAbsent();
        mockCandleAdapter.setCandles(List.of(
            new Candle(Instant.parse("2026-03-10T00:00:00Z"),
                new BigDecimal("68500000"), new BigDecimal("69200000"),
                new BigDecimal("67800000"), new BigDecimal("68900000")),
            new Candle(Instant.parse("2026-03-11T00:00:00Z"),
                new BigDecimal("68900000"), new BigDecimal("70100000"),
                new BigDecimal("68400000"), new BigDecimal("69750000"))
        ));
    }

    @When("캔들 조회 API를 호출한다")
    public void 캔들_조회_API를_호출한다() {
        insertExchangeIfAbsent();
        apiClient.get("/api/candles?exchange=Upbit&coin=BTC&interval=1d");
    }

    @When("유효하지 않은 주기로 캔들 조회 API를 호출한다")
    public void 유효하지_않은_주기로_캔들_조회_API를_호출한다() {
        insertExchangeIfAbsent();
        apiClient.get("/api/candles?exchange=Upbit&coin=BTC&interval=2h");
    }

    @When("거래소 파라미터 없이 캔들 조회 API를 호출한다")
    public void 거래소_파라미터_없이_캔들_조회_API를_호출한다() {
        apiClient.get("/api/candles?coin=BTC&interval=1d");
    }

    @Then("캔들 데이터가 반환된다")
    public void 캔들_데이터가_반환된다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.length()").isEqualTo(2)
            .jsonPath("$.data[0].time").isEqualTo("2026-03-10T00:00:00Z")
            .jsonPath("$.data[0].open").isEqualTo(68500000.0)
            .jsonPath("$.data[1].time").isEqualTo("2026-03-11T00:00:00Z");
    }

    @Then("캔들 데이터가 비어있다")
    public void 캔들_데이터가_비어있다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.length()").isEqualTo(0);
    }

    private void insertExchangeIfAbsent() {
        jdbcTemplate.execute("INSERT IGNORE INTO coin (coin_id, symbol, name) VALUES (1, 'KRW', '원화')");
        jdbcTemplate.execute(
            "INSERT IGNORE INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id, fee_rate) VALUES "
                + "(1, 'Upbit', 'DOMESTIC', 1, 0.000500)");
    }

}
