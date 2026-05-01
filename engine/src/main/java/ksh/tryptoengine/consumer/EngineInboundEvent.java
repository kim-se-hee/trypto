package ksh.tryptoengine.consumer;

public sealed interface EngineInboundEvent
    permits OrderPlacedEvent, OrderCanceledEvent, TickReceivedEvent {
}
