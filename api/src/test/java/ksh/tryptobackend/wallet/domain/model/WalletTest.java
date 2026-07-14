package ksh.tryptobackend.wallet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WalletTest {

    private static final Long ROUND_ID = 1L;
    private static final Long EXCHANGE_ID = 2L;
    private static final Long BTC_COIN_ID = 10L;
    private static final Long ETH_COIN_ID = 11L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 3, 10, 14, 0, 0);

    @Nested
    @DisplayName("상장 코인 확인 (verifyHandles)")
    class VerifyHandlesTest {

        @Test
        @DisplayName("거래소가 취급하는 코인이면 통과한다")
        void verifyHandles_listedCoin_passes() {
            Wallet wallet = wallet();

            assertThatCode(() -> wallet.verifyHandles(BTC_COIN_ID, List.of(BTC_COIN_ID, ETH_COIN_ID)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("거래소가 취급하지 않는 코인이면 COIN_NOT_LISTED_ON_EXCHANGE 예외")
        void verifyHandles_unlistedCoin_throwsException() {
            Wallet wallet = wallet();

            assertThatThrownBy(() -> wallet.verifyHandles(ETH_COIN_ID, List.of(BTC_COIN_ID)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.COIN_NOT_LISTED_ON_EXCHANGE));
        }

        @Test
        @DisplayName("거래소에 상장된 코인이 하나도 없으면 COIN_NOT_LISTED_ON_EXCHANGE 예외")
        void verifyHandles_noListedCoin_throwsException() {
            Wallet wallet = wallet();

            assertThatThrownBy(() -> wallet.verifyHandles(BTC_COIN_ID, List.of()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.COIN_NOT_LISTED_ON_EXCHANGE));
        }
    }

    private Wallet wallet() {
        return Wallet.create(ROUND_ID, EXCHANGE_ID, new BigDecimal("1000000"), CREATED_AT);
    }
}
