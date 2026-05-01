package ksh.tryptoengine.consumer;

public record OrderCanceledEvent(
    Long orderId,
    Long exchangeCoinId
) implements EngineInboundEvent {
}
