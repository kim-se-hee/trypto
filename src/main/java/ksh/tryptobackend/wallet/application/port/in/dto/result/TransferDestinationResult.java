package ksh.tryptobackend.wallet.application.port.in.dto.result;

public record TransferDestinationResult(Long walletId, String failureReason) {

    public static TransferDestinationResult resolved(Long walletId) {
        return new TransferDestinationResult(walletId, null);
    }

    public static TransferDestinationResult failed(String failureReason) {
        return new TransferDestinationResult(null, failureReason);
    }

    public boolean isResolved() {
        return walletId != null;
    }
}
