package ksh.tryptobackend.wallet.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.wallet.application.port.in.dto.command.IssueDepositAddressCommand;

public record GetDepositAddressRequest(
    @NotNull Long coinId
) {

    public IssueDepositAddressCommand toCommand(Long walletId) {
        return new IssueDepositAddressCommand(walletId, coinId);
    }
}
