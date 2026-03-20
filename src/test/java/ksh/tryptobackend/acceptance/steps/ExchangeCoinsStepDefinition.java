package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

public class ExchangeCoinsStepDefinition {

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public ExchangeCoinsStepDefinition(CommonApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before("@exchange-coins")
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM exchange_coin WHERE exchange_coin_id IN (10, 11)");
    }

    @Given("거래소 코인 조회 테스트 데이터가 준비되어 있다")
    public void 거래소_코인_조회_테스트_데이터가_준비되어_있다() {
        insertCoins();
        insertExchanges();
        insertExchangeCoins();
    }

    @When("거래소 {long}의 상장 코인 목록을 조회한다")
    public void 거래소의_상장_코인_목록을_조회한다(Long exchangeId) {
        apiClient.get("/api/exchanges/" + exchangeId + "/coins");
    }

    @Then("상장 코인 개수는 {int}개이다")
    public void 상장_코인_개수는_개이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.length()").isEqualTo(count);
    }

    @Then("첫 번째 상장 코인의 exchangeCoinId는 {long}이다")
    public void 첫_번째_상장_코인의_exchangeCoinId는_이다(Long exchangeCoinId) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[0].exchangeCoinId").isEqualTo(exchangeCoinId);
    }

    @Then("첫 번째 상장 코인의 coinId는 {long}이다")
    public void 첫_번째_상장_코인의_coinId는_이다(Long coinId) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[0].coinId").isEqualTo(coinId);
    }

    @Then("첫 번째 상장 코인의 coinSymbol은 {string}이다")
    public void 첫_번째_상장_코인의_coinSymbol은_이다(String coinSymbol) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[0].coinSymbol").isEqualTo(coinSymbol);
    }

    @Then("첫 번째 상장 코인의 coinName은 {string}이다")
    public void 첫_번째_상장_코인의_coinName은_이다(String coinName) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[0].coinName").isEqualTo(coinName);
    }

    @Then("두 번째 상장 코인의 exchangeCoinId는 {long}이다")
    public void 두_번째_상장_코인의_exchangeCoinId는_이다(Long exchangeCoinId) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[1].exchangeCoinId").isEqualTo(exchangeCoinId);
    }

    @Then("두 번째 상장 코인의 coinSymbol은 {string}이다")
    public void 두_번째_상장_코인의_coinSymbol은_이다(String coinSymbol) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[1].coinSymbol").isEqualTo(coinSymbol);
    }

    private void insertCoins() {
        jdbcTemplate.execute("INSERT IGNORE INTO coin (coin_id, symbol, name) VALUES "
            + "(1, 'KRW', '원화'), "
            + "(2, 'BTC', '비트코인'), "
            + "(3, 'ETH', '이더리움')");
    }

    private void insertExchanges() {
        jdbcTemplate.execute("INSERT IGNORE INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id, fee_rate) VALUES "
            + "(1, 'Upbit', 'DOMESTIC', 1, 0.000500), "
            + "(2, 'Bithumb', 'DOMESTIC', 1, 0.000500)");
    }

    private void insertExchangeCoins() {
        jdbcTemplate.execute("INSERT IGNORE INTO exchange_coin (exchange_coin_id, exchange_id, coin_id, display_name) VALUES "
            + "(10, 1, 2, '비트코인'), "
            + "(11, 1, 3, '이더리움')");
    }
}
