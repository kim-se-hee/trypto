package ksh.tryptobackend.wallet.application.port.in.dto.command;

public record IssueDepositAddressCommand(Long walletId, Long coinId) {
}
