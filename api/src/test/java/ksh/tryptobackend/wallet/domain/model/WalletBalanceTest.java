package ksh.tryptobackend.wallet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WalletBalanceTest {

    private static final Long WALLET_ID = 1L;
    private static final Long COIN_ID = 10L;

    @Test
    @DisplayName("가용 잔고 차감 — 가용 잔고가 줄어든다")
    void deductAvailable() {
        WalletBalance balance = balance("2.0", "0");

        balance.deductAvailable(new BigDecimal("1.5"));

        assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("가용 잔고 부족 차감 — INSUFFICIENT_BALANCE 예외")
    void deductAvailable_insufficient_throwsException() {
        WalletBalance balance = balance("1.0", "0");

        assertThatThrownBy(() -> balance.deductAvailable(new BigDecimal("1.5")))
                .isInstanceOf(CustomException.class)
                .satisfies(
                        e ->
                                assertThat(((CustomException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    @DisplayName("잠금 — 가용 잔고가 잠금 잔고로 이동한다")
    void lock() {
        WalletBalance balance = balance("2.0", "0");

        balance.lock(new BigDecimal("1.5"));

        assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(balance.getLocked()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("잠금 해제 — 잠금 잔고가 가용 잔고로 복귀한다")
    void unlock() {
        WalletBalance balance = balance("0.5", "1.5");

        balance.unlock(new BigDecimal("1.5"));

        assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(balance.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("잠금 잔고 부족 해제 — INSUFFICIENT_BALANCE 예외")
    void unlock_insufficient_throwsException() {
        WalletBalance balance = balance("0.5", "1.0");

        assertThatThrownBy(() -> balance.unlock(new BigDecimal("1.5")))
                .isInstanceOf(CustomException.class)
                .satisfies(
                        e ->
                                assertThat(((CustomException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    @DisplayName("잠금 소진 — 잠금 잔고만 줄어든다")
    void consumeLocked() {
        WalletBalance balance = balance("0.5", "1.5");

        balance.consumeLocked(new BigDecimal("1.5"));

        assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(balance.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("잠금 잔고 부족 소진 — INSUFFICIENT_BALANCE 예외")
    void consumeLocked_insufficient_throwsException() {
        WalletBalance balance = balance("0.5", "1.0");

        assertThatThrownBy(() -> balance.consumeLocked(new BigDecimal("1.5")))
                .isInstanceOf(CustomException.class)
                .satisfies(
                        e ->
                                assertThat(((CustomException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
    }

    private WalletBalance balance(String available, String locked) {
        return WalletBalance.builder()
                .id(1L)
                .walletId(WALLET_ID)
                .coinId(COIN_ID)
                .available(new BigDecimal(available))
                .locked(new BigDecimal(locked))
                .build();
    }
}
