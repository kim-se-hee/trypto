package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

public class CoinChainStepDefinition {

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public CoinChainStepDefinition(CommonApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before("@coin-chains")
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM exchange_coin_chain WHERE exchange_coin_chain_id IN (1, 2, 3)");
        jdbcTemplate.execute("DELETE FROM exchange_coin WHERE exchange_coin_id IN (102, 103, 104)");
    }

    @Given("코인 체인 조회용 데이터가 준비되어 있다")
    public void 코인_체인_조회용_데이터가_준비되어_있다() {
        insertExchange();
        insertExchangeCoins();
        insertExchangeCoinChains();
    }

    @When("거래소 {long}의 코인 {long}에 대한 체인 목록을 조회한다")
    public void 거래소의_코인에_대한_체인_목록을_조회한다(Long exchangeId, Long coinId) {
        apiClient.get("/api/exchanges/" + exchangeId + "/coins/" + coinId + "/chains");
    }

    @Then("응답 메시지는 {string}이다")
    public void 응답_메시지는_이다(String message) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.message").isEqualTo(message);
    }

    @Then("체인 목록의 크기는 {int}이다")
    public void 체인_목록의_크기는_이다(int size) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.length()").isEqualTo(size);
    }

    @Then("첫 번째 체인의 chain은 {string}이다")
    public void 첫_번째_체인의_chain은_이다(String chain) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[0].chain").isEqualTo(chain);
    }

    @Then("첫 번째 체인은 태그가 필요하다")
    public void 첫_번째_체인은_태그가_필요하다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[0].tagRequired").isEqualTo(true);
    }

    @Then("첫 번째 체인은 태그가 필요하지 않다")
    public void 첫_번째_체인은_태그가_필요하지_않다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data[0].tagRequired").isEqualTo(false);
    }

    private void insertExchange() {
        jdbcTemplate.execute(
            "INSERT IGNORE INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id, fee_rate) VALUES "
                + "(1, 'Upbit', 'DOMESTIC', 1, 0.000500)");
    }

    private void insertExchangeCoins() {
        jdbcTemplate.execute(
            "INSERT INTO exchange_coin (exchange_coin_id, exchange_id, coin_id, display_name) VALUES "
                + "(102, 1, 2, '비트코인'), "
                + "(103, 1, 3, '리플'), "
                + "(104, 1, 4, '이더리움')");
    }

    private void insertExchangeCoinChains() {
        jdbcTemplate.execute(
            "INSERT INTO exchange_coin_chain (exchange_coin_chain_id, exchange_coin_id, chain, tag_required) VALUES "
                + "(1, 102, 'ERC-20', false), "
                + "(2, 102, 'BEP-20', false), "
                + "(3, 103, 'Ripple', true)");
    }
}
