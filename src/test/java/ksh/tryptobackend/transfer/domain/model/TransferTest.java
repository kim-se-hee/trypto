package ksh.tryptobackend.transfer.domain.model;

import ksh.tryptobackend.transfer.domain.vo.TransferBalanceChange;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferTest {

    private static final Long FROM_WALLET_ID = 1L;
    private static final Long TO_WALLET_ID = 2L;
    private static final Long COIN_ID = 10L;

    @Nested
    @DisplayName("잔고 변동 계획 (planBalanceChanges)")
    class PlanBalanceChangesTest {

        @Test
        @DisplayName("SUCCESS 상태 — Deduct(수수료 포함)와 Add(수수료 미포함) 2건 반환")
        void planBalanceChanges_success_returnsDeductAndAdd() {
            // given
            BigDecimal amount = new BigDecimal("1.5");
            BigDecimal fee = new BigDecimal("0.001");
            Transfer transfer = createTransfer(amount, fee, TransferStatus.SUCCESS);

            // when
            List<TransferBalanceChange> changes = transfer.planBalanceChanges();

            // then
            assertThat(changes).hasSize(2);

            TransferBalanceChange.Deduct deduct = (TransferBalanceChange.Deduct) changes.get(0);
            assertThat(deduct.walletId()).isEqualTo(FROM_WALLET_ID);
            assertThat(deduct.coinId()).isEqualTo(COIN_ID);
            assertThat(deduct.amount()).isEqualByComparingTo(new BigDecimal("1.501"));

            TransferBalanceChange.Add add = (TransferBalanceChange.Add) changes.get(1);
            assertThat(add.walletId()).isEqualTo(TO_WALLET_ID);
            assertThat(add.coinId()).isEqualTo(COIN_ID);
            assertThat(add.amount()).isEqualByComparingTo(new BigDecimal("1.5"));
        }

        @Test
        @DisplayName("SUCCESS 상태 — 수수료가 0인 경우 Deduct 금액은 amount와 동일")
        void planBalanceChanges_successZeroFee_deductEqualsAmount() {
            // given
            BigDecimal amount = new BigDecimal("10");
            BigDecimal fee = BigDecimal.ZERO;
            Transfer transfer = createTransfer(amount, fee, TransferStatus.SUCCESS);

            // when
            List<TransferBalanceChange> changes = transfer.planBalanceChanges();

            // then
            TransferBalanceChange.Deduct deduct = (TransferBalanceChange.Deduct) changes.get(0);
            assertThat(deduct.amount()).isEqualByComparingTo(new BigDecimal("10"));

            TransferBalanceChange.Add add = (TransferBalanceChange.Add) changes.get(1);
            assertThat(add.amount()).isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("FROZEN 상태 — Lock(수수료 포함) 1건만 반환")
        void planBalanceChanges_frozen_returnsLockOnly() {
            // given
            BigDecimal amount = new BigDecimal("1.5");
            BigDecimal fee = new BigDecimal("0.001");
            Transfer transfer = createTransfer(amount, fee, TransferStatus.FROZEN);

            // when
            List<TransferBalanceChange> changes = transfer.planBalanceChanges();

            // then
            assertThat(changes).hasSize(1);

            TransferBalanceChange.Lock lock = (TransferBalanceChange.Lock) changes.get(0);
            assertThat(lock.walletId()).isEqualTo(FROM_WALLET_ID);
            assertThat(lock.coinId()).isEqualTo(COIN_ID);
            assertThat(lock.amount()).isEqualByComparingTo(new BigDecimal("1.501"));
        }

        @Test
        @DisplayName("FROZEN 상태 — toWalletId 쪽에는 아무 변동이 없다")
        void planBalanceChanges_frozen_noChangeForToWallet() {
            // given
            Transfer transfer = createTransfer(
                    new BigDecimal("5"), new BigDecimal("0.01"), TransferStatus.FROZEN);

            // when
            List<TransferBalanceChange> changes = transfer.planBalanceChanges();

            // then
            boolean hasToWalletChange = changes.stream()
                    .anyMatch(change -> switch (change) {
                        case TransferBalanceChange.Deduct d -> d.walletId().equals(TO_WALLET_ID);
                        case TransferBalanceChange.Add a -> a.walletId().equals(TO_WALLET_ID);
                        case TransferBalanceChange.Lock l -> l.walletId().equals(TO_WALLET_ID);
                    });
            assertThat(hasToWalletChange).isFalse();
        }

        @Test
        @DisplayName("REFUNDED 상태 — IllegalStateException 발생")
        void planBalanceChanges_refunded_throwsIllegalStateException() {
            // given
            Transfer transfer = createTransfer(
                    new BigDecimal("1"), new BigDecimal("0.001"), TransferStatus.REFUNDED);

            // when & then
            assertThatThrownBy(transfer::planBalanceChanges)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REFUNDED");
        }
    }

    private Transfer createTransfer(BigDecimal amount, BigDecimal fee, TransferStatus status) {
        return Transfer.builder()
                .fromWalletId(FROM_WALLET_ID)
                .toWalletId(TO_WALLET_ID)
                .coinId(COIN_ID)
                .amount(amount)
                .fee(fee)
                .status(status)
                .build();
    }
}
