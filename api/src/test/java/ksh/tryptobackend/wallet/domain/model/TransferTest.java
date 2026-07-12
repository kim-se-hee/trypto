package ksh.tryptobackend.wallet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.wallet.domain.vo.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            TransferCoinCommand command = command(TO_WALLET_ID, new BigDecimal("1.5"));

            Transfer transfer = Transfer.create(command, CREATED_AT);

            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.SUCCESS);
            assertThat(transfer.getCompletedAt()).isEqualTo(CREATED_AT);
            assertThat(transfer.getAmount()).isEqualByComparingTo(new BigDecimal("1.5"));
        }

        @Test
        @DisplayName("같은 지갑 송금 — SAME_WALLET_TRANSFER 예외")
        void create_sameWallet_throwsException() {
            TransferCoinCommand command = command(FROM_WALLET_ID, new BigDecimal("1.0"));

            assertThatThrownBy(() -> Transfer.create(command, CREATED_AT))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e ->
                            assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.SAME_WALLET_TRANSFER));
        }
    }

    private TransferCoinCommand command(Long toWalletId, BigDecimal amount) {
        return new TransferCoinCommand(UUID.randomUUID().toString(), 1L, FROM_WALLET_ID, toWalletId, COIN_ID, amount);
    }
}
