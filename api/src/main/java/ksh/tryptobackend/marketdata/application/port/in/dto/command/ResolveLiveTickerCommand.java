package ksh.tryptobackend.marketdata.application.port.in.dto.command;

import java.util.List;

public record ResolveLiveTickerCommand(String exchange, List<ExternalTickerCommand> tickers) {}
