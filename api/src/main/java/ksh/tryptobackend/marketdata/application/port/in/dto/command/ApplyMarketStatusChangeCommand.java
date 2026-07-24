package ksh.tryptobackend.marketdata.application.port.in.dto.command;

import ksh.tryptobackend.marketdata.domain.model.MarketStatus;

public record ApplyMarketStatusChangeCommand(
        String exchange, String baseSymbol, String displayName, MarketStatus status) {}
