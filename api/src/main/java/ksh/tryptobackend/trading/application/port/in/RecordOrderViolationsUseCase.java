package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;

public interface RecordOrderViolationsUseCase {

    void record(OrderPlacedEvent event);
}
