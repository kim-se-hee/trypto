package ksh.tryptobackend.trading.domain.event;

import ksh.tryptobackend.trading.domain.model.Order;

public record OrderPlacedEvent(Order order) {
}
