package ksh.tryptobackend.acceptance.steps.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;

public class TransferStepDefinition {

    private static final Long ROUND_ID = 500L;
    private static final Long FROM_WALLET_ID = 501L;
    private static final Long TO_WALLET_ID = 502L;
    private static final Long OTHER_ROUND_ID = 600L;
    private static final Long OTHER_ROUND_WALLET_ID = 601L;
    private static final Long EXCHANGE_ID_1 = 1L;
    private static final Long EXCHANGE_ID_2 = 2L;
    private static final Long COIN_ID = 2L;
    private static final String COIN_SYMBOL = "BTC";

    private final CommonApiClient apiClient;
    private final WalletBalanceJpaRepository walletBalanceJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    private Long lastTransferId;
    private Long firstTransferId;

    public TransferStepDefinition(
            CommonApiClient apiClient,
            WalletBalanceJpaRepository walletBalanceJpaRepository,
            JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.walletBalanceJpaRepository = walletBalanceJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("송금용 동일 라운드의 두 지갑이 준비되어 있다")
    public void 송금용_동일_라운드의_두_지갑이_준비되어_있다() {
        createRound(ROUND_ID);
        createWallet(FROM_WALLET_ID, ROUND_ID, EXCHANGE_ID_1);
        createWallet(TO_WALLET_ID, ROUND_ID, EXCHANGE_ID_2);
    }

    @Given("송금용 코인이 등록되어 있다")
    public void 송금용_코인이_등록되어_있다() {}

    @Given("출금 지갑에 BTC 잔고가 {double}개 있다")
    public void 출금_지갑에_BTC_잔고가_개_있다(double amount) {
        walletBalanceJpaRepository.save(
                new ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity(
                        FROM_WALLET_ID,
                        COIN_ID,
                        new BigDecimal(String.valueOf(amount)),
                        BigDecimal.ZERO));
    }

    @Given("다른 라운드의 지갑이 준비되어 있다")
    public void 다른_라운드의_지갑이_준비되어_있다() {
        createRound(OTHER_ROUND_ID);
        createWallet(OTHER_ROUND_WALLET_ID, OTHER_ROUND_ID, EXCHANGE_ID_1);
    }

    @When("출금 지갑에서 입금 지갑으로 BTC {double}개를 송금한다")
    public void 출금_지갑에서_입금_지갑으로_BTC_개를_송금한다(double amount) {
        Map<String, Object> body = createTransferBody(FROM_WALLET_ID, TO_WALLET_ID, amount);
        apiClient.post("/api/transfers", body);
        extractTransferIdIfSuccess();
    }

    @When("출금 지갑에서 출금 지갑으로 BTC {double}개를 송금한다")
    public void 출금_지갑에서_출금_지갑으로_BTC_개를_송금한다(double amount) {
        Map<String, Object> body = createTransferBody(FROM_WALLET_ID, FROM_WALLET_ID, amount);
        apiClient.post("/api/transfers", body);
    }

    @When("출금 지갑에서 다른 라운드 지갑으로 BTC {double}개를 송금한다")
    public void 출금_지갑에서_다른_라운드_지갑으로_BTC_개를_송금한다(double amount) {
        Map<String, Object> body =
                createTransferBody(FROM_WALLET_ID, OTHER_ROUND_WALLET_ID, amount);
        apiClient.post("/api/transfers", body);
    }

    @When("출금 지갑에서 존재하지 않는 지갑으로 BTC {double}개를 송금한다")
    public void 출금_지갑에서_존재하지_않는_지갑으로_BTC_개를_송금한다(double amount) {
        Map<String, Object> body = createTransferBody(FROM_WALLET_ID, 999999L, amount);
        apiClient.post("/api/transfers", body);
    }

    @When("동일한 idempotencyKey로 BTC {double}개를 {int}번 송금한다")
    public void 동일한_idempotencyKey로_BTC_개를_N번_송금한다(double amount, int count) {
        String idempotencyKey = UUID.randomUUID().toString();
        for (int i = 0; i < count; i++) {
            Map<String, Object> body = new HashMap<>();
            body.put("idempotencyKey", idempotencyKey);
            body.put("fromWalletId", FROM_WALLET_ID);
            body.put("toWalletId", TO_WALLET_ID);
            body.put("coinId", COIN_ID);
            body.put("amount", new BigDecimal(String.valueOf(amount)));
            apiClient.post("/api/transfers", body);
            extractTransferIdIfSuccess();
            if (i == 0) {
                firstTransferId = lastTransferId;
            }
        }
    }

    @Then("송금 상태는 {string}이다")
    public void 송금_상태는_이다(String status) {
        apiClient.getLastResponse().expectBody().jsonPath("$.data.status").isEqualTo(status);
    }

    @Then("두 응답의 transferId가 동일하다")
    public void 두_응답의_transferId가_동일하다() {
        assertThat(firstTransferId).isEqualTo(lastTransferId);
    }

    private Map<String, Object> createTransferBody(
            Long fromWalletId, Long toWalletId, double amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("idempotencyKey", UUID.randomUUID().toString());
        body.put("fromWalletId", fromWalletId);
        body.put("toWalletId", toWalletId);
        body.put("coinId", COIN_ID);
        body.put("amount", new BigDecimal(String.valueOf(amount)));
        return body;
    }

    private void createRound(Long roundId) {
        jdbcTemplate.update(
                "INSERT INTO investment_round (round_id, version, user_id, round_number,"
                        + " initial_seed, emergency_funding_limit, emergency_charge_count, status,"
                        + " started_at) VALUES (?, 0, ?, 1, 10000000, 1000000, 0, 'ACTIVE', ?)",
                roundId,
                roundId,
                LocalDateTime.now());
    }

    private void createWallet(Long walletId, Long roundId, Long exchangeId) {
        jdbcTemplate.update(
                "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at) "
                        + "VALUES (?, ?, ?, 10000000, ?)",
                walletId,
                roundId,
                exchangeId,
                LocalDateTime.now());
    }

    @SuppressWarnings("unchecked")
    private void extractTransferIdIfSuccess() {
        Map<String, Object> body =
                apiClient.getLastResponse().expectBody(Map.class).returnResult().getResponseBody();
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data != null && data.get("transferId") instanceof Number num) {
            lastTransferId = num.longValue();
        }
    }
}
