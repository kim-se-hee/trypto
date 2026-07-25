package ksh.tryptocollector.ingest.binance;

import java.util.List;

public record BinanceExchangeInfoResponse(List<Symbol> symbols) {

    public record Symbol(String symbol, String status) {}
}
