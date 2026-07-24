package ksh.tryptobackend.marketdata.adapter.in.dto;

import ksh.tryptobackend.marketdata.domain.vo.MarketStatus;

public record MarketStatusChangedMessage(String exchange, String symbol, String displayName, MarketStatus status) {}
