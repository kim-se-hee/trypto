package ksh.tryptobackend.transfer.domain.vo;

public sealed interface TransferDestination {

    record Resolved(Long walletId) implements TransferDestination {}

    record Failed(TransferFailureReason reason) implements TransferDestination {}
}
