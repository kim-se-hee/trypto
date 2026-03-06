package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.mock.MockTransferWalletAdapter;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.transfer.adapter.out.entity.TransferJpaEntity;
import ksh.tryptobackend.transfer.adapter.out.repository.TransferJpaRepository;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferFailureReason;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferHistoryStepDefinition {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long WALLET_ID = 100L;
    private static final Long OTHER_WALLET_ID = 200L;
    private static final Long COIN_ID = 1L;

    private final CommonApiClient apiClient;
    private final MockTransferWalletAdapter transferWalletAdapter;
    private final TransferJpaRepository transferJpaRepository;

    private Long walletId;

    public TransferHistoryStepDefinition(
        CommonApiClient apiClient,
        MockTransferWalletAdapter transferWalletAdapter,
        TransferJpaRepository transferJpaRepository
    ) {
        this.apiClient = apiClient;
        this.transferWalletAdapter = transferWalletAdapter;
        this.transferJpaRepository = transferJpaRepository;
    }

    @Before("@transfer-history")
    public void setUp() {
        transferJpaRepository.deleteAllInBatch();
        transferWalletAdapter.clear();
        walletId = null;
    }

    @Given("송금 내역 조회용 지갑과 송금 데이터가 준비되어 있다")
    public void 송금_내역_조회용_지갑과_송금_데이터가_준비되어_있다() {
        transferWalletAdapter.setOwnerUserId(WALLET_ID, USER_ID);
        transferWalletAdapter.setOwnerUserId(OTHER_WALLET_ID, OTHER_USER_ID);

        LocalDateTime now = LocalDateTime.now();

        // WITHDRAW: WALLET_ID -> OTHER_WALLET_ID (SUCCESS)
        saveTransfer(Transfer.builder()
            .idempotencyKey(UUID.randomUUID())
            .fromWalletId(WALLET_ID)
            .toWalletId(OTHER_WALLET_ID)
            .coinId(COIN_ID)
            .chain("ERC-20")
            .toAddress("0xabc123")
            .amount(new BigDecimal("0.01"))
            .fee(new BigDecimal("0.0005"))
            .status(TransferStatus.SUCCESS)
            .createdAt(now.minusHours(3))
            .build());

        // DEPOSIT: OTHER_WALLET_ID -> WALLET_ID (SUCCESS)
        saveTransfer(Transfer.builder()
            .idempotencyKey(UUID.randomUUID())
            .fromWalletId(OTHER_WALLET_ID)
            .toWalletId(WALLET_ID)
            .coinId(COIN_ID)
            .chain("Bitcoin")
            .toAddress("bc1qxyz")
            .amount(new BigDecimal("0.005"))
            .fee(new BigDecimal("0.0003"))
            .status(TransferStatus.SUCCESS)
            .createdAt(now.minusHours(2))
            .build());

        // WITHDRAW: WALLET_ID -> (FROZEN)
        saveTransfer(Transfer.builder()
            .idempotencyKey(UUID.randomUUID())
            .fromWalletId(WALLET_ID)
            .coinId(COIN_ID)
            .chain("ERC-20")
            .toAddress("0xinvalid")
            .amount(new BigDecimal("0.008"))
            .fee(new BigDecimal("0.0005"))
            .status(TransferStatus.FROZEN)
            .failureReason(TransferFailureReason.WRONG_ADDRESS)
            .frozenUntil(now.minusHours(1).plusHours(24))
            .createdAt(now.minusHours(1))
            .build());

        walletId = WALLET_ID;
    }

    @When("지갑의 송금 내역을 ALL 타입으로 조회한다")
    public void 지갑의_송금_내역을_ALL_타입으로_조회한다() {
        apiClient.get("/api/wallets/" + walletId + "/transfers?userId=" + USER_ID + "&type=ALL");
    }

    @When("지갑의 송금 내역을 DEPOSIT 타입으로 조회한다")
    public void 지갑의_송금_내역을_DEPOSIT_타입으로_조회한다() {
        apiClient.get("/api/wallets/" + walletId + "/transfers?userId=" + USER_ID + "&type=DEPOSIT");
    }

    @When("지갑의 송금 내역을 WITHDRAW 타입으로 조회한다")
    public void 지갑의_송금_내역을_WITHDRAW_타입으로_조회한다() {
        apiClient.get("/api/wallets/" + walletId + "/transfers?userId=" + USER_ID + "&type=WITHDRAW");
    }

    @When("지갑의 송금 내역을 size {int}로 조회한다")
    public void 지갑의_송금_내역을_size로_조회한다(int size) {
        apiClient.get("/api/wallets/" + walletId + "/transfers?userId=" + USER_ID + "&size=" + size);
    }

    @When("다른 사용자의 지갑으로 송금 내역을 조회한다")
    public void 다른_사용자의_지갑으로_송금_내역을_조회한다() {
        apiClient.get("/api/wallets/" + WALLET_ID + "/transfers?userId=" + OTHER_USER_ID);
    }

    @When("존재하지 않는 지갑으로 송금 내역을 조회한다")
    public void 존재하지_않는_지갑으로_송금_내역을_조회한다() {
        apiClient.get("/api/wallets/999999/transfers?userId=" + USER_ID);
    }

    @Then("송금 내역이 {int}건 조회된다")
    public void 송금_내역이_N건_조회된다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.content.length()").isEqualTo(count);
    }

    @Then("송금 내역에 DEPOSIT과 WITHDRAW가 모두 포함된다")
    public void 송금_내역에_DEPOSIT과_WITHDRAW가_모두_포함된다() {
        byte[] body = apiClient.getLastResponse()
            .expectBody().returnResult().getResponseBody();
        List<String> types = com.jayway.jsonpath.JsonPath.read(new String(body), "$.data.content[*].type");
        assertThat(types).contains("DEPOSIT", "WITHDRAW");
    }

    @Then("송금 내역의 타입이 모두 {string}이다")
    public void 송금_내역의_타입이_모두_이다(String expectedType) {
        byte[] body = apiClient.getLastResponse()
            .expectBody().returnResult().getResponseBody();
        List<String> types = com.jayway.jsonpath.JsonPath.read(new String(body), "$.data.content[*].type");
        assertThat(types).isNotEmpty().allMatch(type -> type.equals(expectedType));
    }

    private void saveTransfer(Transfer transfer) {
        transferJpaRepository.save(TransferJpaEntity.fromDomain(transfer));
    }
}
