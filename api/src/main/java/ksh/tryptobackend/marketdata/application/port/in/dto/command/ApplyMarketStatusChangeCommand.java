package ksh.tryptobackend.marketdata.application.port.in.dto.command;

import ksh.tryptobackend.marketdata.domain.vo.MarketStatus;

public record ApplyMarketStatusChangeCommand(
        String exchange, String baseSymbol, String displayName, MarketStatus status) {}
