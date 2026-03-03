package ksh.tryptobackend.wallet.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.wallet.application.port.in.dto.query.GetDepositAddressQuery;

public record GetDepositAddressRequest(
    @NotNull Long coinId,
    @NotBlank String chain
) {

    public GetDepositAddressQuery toQuery(Long walletId) {
        return new GetDepositAddressQuery(walletId, coinId, chain);
    }
}
