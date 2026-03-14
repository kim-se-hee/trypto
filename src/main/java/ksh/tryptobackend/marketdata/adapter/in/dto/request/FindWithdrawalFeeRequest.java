package ksh.tryptobackend.marketdata.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FindWithdrawalFeeRequest(
    @NotNull Long exchangeId,
    @NotNull Long coinId,
    @NotBlank String chain
) {
}
