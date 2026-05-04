package ksh.tryptobackend.marketdata.application.port.in.dto.result;

public record CoinChainResult(Long exchangeCoinChainId, String chain, boolean tagRequired) {}
