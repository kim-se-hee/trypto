package ksh.tryptobackend.marketdata.application.port.in.dto.command;

import java.math.BigDecimal;

public record ExternalTickerCommand(
        String symbol, BigDecimal currentPrice, BigDecimal changeRate, BigDecimal quoteTurnover, Long timestamp) {}
