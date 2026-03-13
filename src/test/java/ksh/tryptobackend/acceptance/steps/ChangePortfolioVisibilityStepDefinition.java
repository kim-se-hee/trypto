package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;

public class ChangePortfolioVisibilityStepDefinition {

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public ChangePortfolioVisibilityStepDefinition(CommonApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM user WHERE user_id IN (1, 2)");
    }

    @Given("포트폴리오 공개 설정 테스트 데이터가 준비되어 있다")
    public void 포트폴리오_공개_설정_테스트_데이터가_준비되어_있다() {
        insertUsers();
    }

    @When("유저 {long}의 포트폴리오 공개 설정을 {word}로 변경한다")
    public void 유저의_포트폴리오_공개_설정을_변경한다(Long userId, String visibility) {
        Map<String, Boolean> requestBody = Map.of("portfolioPublic", Boolean.parseBoolean(visibility));
        apiClient.put("/api/users/" + userId + "/portfolio-visibility", requestBody);
    }

    @Then("응답의 portfolioPublic은 {word}이다")
    public void 응답의_portfolioPublic은_이다(String expected) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.portfolioPublic").isEqualTo(Boolean.parseBoolean(expected));
    }

    private void insertUsers() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT IGNORE INTO user (user_id, email, nickname, portfolio_public, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            1L, "trader1@test.com", "트레이더1", true, now, now);
        jdbcTemplate.update(
            "INSERT IGNORE INTO user (user_id, email, nickname, portfolio_public, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            2L, "trader2@test.com", "트레이더2", false, now, now);
    }
}
