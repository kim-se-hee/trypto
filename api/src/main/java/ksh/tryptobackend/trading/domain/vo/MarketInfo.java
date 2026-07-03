package ksh.tryptobackend.trading.domain.vo;

public record MarketInfo(TradingPair tradingPair, ExchangeInfo exchangeInfo, Price currentPrice) {}
