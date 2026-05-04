package ksh.tryptobackend.marketdata.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawalFee {

    private final Long withdrawalFeeId;
    private final Long exchangeId;
    private final Long coinId;
    private final String chain;
    private final BigDecimal fee;
    private final BigDecimal minWithdrawal;
}
