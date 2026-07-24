package ksh.tryptobackend.marketdata.adapter.in.dto;

import ksh.tryptobackend.marketdata.domain.model.MarketStatus;

public record MarketStatusChangedMessage(String exchange, String symbol, String displayName, MarketStatus status) {}
