package ksh.tryptoengine.consumer;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TickReceivedEvent(
        String exchange, @JsonAlias("displayName") String symbol, BigDecimal tradePrice, LocalDateTime tickAt)
        implements EngineInboundEvent {}
