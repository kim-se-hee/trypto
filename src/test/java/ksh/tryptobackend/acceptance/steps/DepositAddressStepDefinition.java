package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.DepositAddressJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.domain.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class DepositAddressStepDefinition {

    private static final Long EXCHANGE_ID = 1L;
    private static final Long ROUND_ID = 1L;
    private static final Long KRW_COIN_ID = 1L;
    private static final Long BTC_COIN_ID = 2L;

    private final CommonApiClient apiClient;
    private final ExchangeJpaRepository exchangeJpaRepository;
    private final WalletJpaRepository walletJpaRepository;
    private final DepositAddressJpaRepository depositAddressJpaRepository;

    private Long walletId;
    private String previousAddress;

    public DepositAddressStepDefinition(
        CommonApiClient apiClient,
        ExchangeJpaRepository exchangeJpaRepository,
        WalletJpaRepository walletJpaRepository,
        DepositAddressJpaRepository depositAddressJpaRepository
    ) {
        this.apiClient = apiClient;
        this.exchangeJpaRepository = exchangeJpaRepository;
        this.walletJpaRepository = walletJpaRepository;
        this.depositAddressJpaRepository = depositAddressJpaRepository;
    }

    @Before("@deposit-address")
    public void setUp() {
        depositAddressJpaRepository.deleteAllInBatch();
        walletJpaRepository.deleteAllInBatch();
        exchangeJpaRepository.deleteAllInBatch();
        walletId = null;
        previousAddress = null;
    }

    @Given("입금 주소용 거래소와 지갑이 준비되어 있다")
    public void 입금_주소용_거래소와_지갑이_준비되어_있다() {
        exchangeJpaRepository.save(new ExchangeJpaEntity(
            EXCHANGE_ID, "Upbit", ExchangeMarketType.DOMESTIC,
            KRW_COIN_ID, new BigDecimal("0.0005")));

        Wallet wallet = Wallet.create(ROUND_ID, EXCHANGE_ID, BigDecimal.ZERO, LocalDateTime.now());
        WalletJpaEntity walletEntity = WalletJpaEntity.fromDomain(wallet);
        walletId = walletJpaRepository.save(walletEntity).getId();
    }

    @When("지갑의 BTC 입금 주소를 조회한다")
    public void 지갑의_BTC_입금_주소를_조회한다() {
        savePreviousAddress();
        apiClient.get("/api/wallets/" + walletId + "/deposit-address?coinId=" + BTC_COIN_ID);
    }

    @When("지갑의 KRW 입금 주소를 조회한다")
    public void 지갑의_KRW_입금_주소를_조회한다() {
        apiClient.get("/api/wallets/" + walletId + "/deposit-address?coinId=" + KRW_COIN_ID);
    }

    @When("존재하지 않는 지갑의 입금 주소를 조회한다")
    public void 존재하지_않는_지갑의_입금_주소를_조회한다() {
        apiClient.get("/api/wallets/999999/deposit-address?coinId=" + BTC_COIN_ID);
    }

    @Then("입금 주소의 address가 존재한다")
    public void 입금_주소의_address가_존재한다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.address").isNotEmpty();
    }

    @Then("이전과 동일한 입금 주소가 반환된다")
    public void 이전과_동일한_입금_주소가_반환된다() {
        byte[] body = apiClient.getLastResponse()
            .expectBody().returnResult().getResponseBody();
        String currentAddress = com.jayway.jsonpath.JsonPath.read(new String(body), "$.data.address");
        assertThat(currentAddress).isEqualTo(previousAddress);
    }

    private void savePreviousAddress() {
        if (previousAddress != null) {
            return;
        }
        try {
            byte[] body = apiClient.getLastResponse()
                .expectBody().returnResult().getResponseBody();
            if (body != null) {
                previousAddress = com.jayway.jsonpath.JsonPath.read(new String(body), "$.data.address");
            }
        } catch (Exception ignored) {
            // No previous response
        }
    }
}
