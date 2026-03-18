package ksh.tryptobackend.transfer.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.transfer.domain.vo.TransferBalanceChange;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferTest {

    private static final Long FROM_WALLET_ID = 1L;
    private static final Long TO_WALLET_ID = 2L;
    private static final Long COIN_ID = 10L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 3, 10, 14, 0, 0);

    @Nested
    @DisplayName("송금 생성 (create)")
    class CreateTest {

        @Test
        @DisplayName("정상 생성 — SUCCESS 상태, completedAt이 createdAt과 동일")
        void create_success() {
            Transfer transfer = Transfer.create(
                UUID.randomUUID(), FROM_WALLET_ID, TO_WALLET_ID,
                COIN_ID, new BigDecimal("1.5"), CREATED_AT);

            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.SUCCESS);
            assertThat(transfer.getCompletedAt()).isEqualTo(CREATED_AT);
            assertThat(transfer.getAmount()).isEqualByComparingTo(new BigDecimal("1.5"));
        }

        @Test
        @DisplayName("같은 지갑 송금 — SAME_WALLET_TRANSFER 예외")
        void create_sameWallet_throwsException() {
            assertThatThrownBy(() -> Transfer.create(
                UUID.randomUUID(), FROM_WALLET_ID, FROM_WALLET_ID,
                COIN_ID, new BigDecimal("1.0"), CREATED_AT))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SAME_WALLET_TRANSFER));
        }
    }

    @Nested
    @DisplayName("잔고 변동 계획 (planBalanceChanges)")
    class PlanBalanceChangesTest {

        @Test
        @DisplayName("Deduct와 Add 2건 반환, 금액은 amount 그대로")
        void planBalanceChanges_returnsDeductAndAdd() {
            BigDecimal amount = new BigDecimal("1.5");
            Transfer transfer = createTransfer(amount);

            List<TransferBalanceChange> changes = transfer.planBalanceChanges();

            assertThat(changes).hasSize(2);

            TransferBalanceChange.Deduct deduct = (TransferBalanceChange.Deduct) changes.get(0);
            assertThat(deduct.walletId()).isEqualTo(FROM_WALLET_ID);
            assertThat(deduct.coinId()).isEqualTo(COIN_ID);
            assertThat(deduct.amount()).isEqualByComparingTo(new BigDecimal("1.5"));

            TransferBalanceChange.Add add = (TransferBalanceChange.Add) changes.get(1);
            assertThat(add.walletId()).isEqualTo(TO_WALLET_ID);
            assertThat(add.coinId()).isEqualTo(COIN_ID);
            assertThat(add.amount()).isEqualByComparingTo(new BigDecimal("1.5"));
        }
    }

    @Nested
    @DisplayName("총 차감액 (getTotalDeduction)")
    class GetTotalDeductionTest {

        @Test
        @DisplayName("수수료 없이 amount 그대로 반환")
        void getTotalDeduction_returnsAmount() {
            Transfer transfer = createTransfer(new BigDecimal("10"));

            assertThat(transfer.getTotalDeduction()).isEqualByComparingTo(new BigDecimal("10"));
        }
    }

    private Transfer createTransfer(BigDecimal amount) {
        return Transfer.builder()
            .fromWalletId(FROM_WALLET_ID)
            .toWalletId(TO_WALLET_ID)
            .coinId(COIN_ID)
            .amount(amount)
            .status(TransferStatus.SUCCESS)
            .build();
    }
}
