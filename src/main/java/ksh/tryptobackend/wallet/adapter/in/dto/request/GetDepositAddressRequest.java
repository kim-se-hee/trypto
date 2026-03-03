package ksh.tryptobackend.wallet.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.wallet.application.port.in.dto.command.IssueDepositAddressCommand;

public record GetDepositAddressRequest(
    @NotNull Long coinId,
    @NotBlank String chain
) {

    public IssueDepositAddressCommand toCommand(Long walletId) {
        return new IssueDepositAddressCommand(walletId, coinId, chain);
    }
}
