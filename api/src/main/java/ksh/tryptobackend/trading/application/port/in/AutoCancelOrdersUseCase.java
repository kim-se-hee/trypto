package ksh.tryptobackend.trading.application.port.in;

public interface AutoCancelOrdersUseCase {

    void autoCancel(Long exchangeCoinId);
}
