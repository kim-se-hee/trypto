package ksh.tryptobackend.trading.application.port.in;

public interface RecalculateHoldingUseCase {

    void recalculate(Long walletId, Long exchangeCoinId);
}
