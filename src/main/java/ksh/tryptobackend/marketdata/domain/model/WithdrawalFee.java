package ksh.tryptobackend.marketdata.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

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
