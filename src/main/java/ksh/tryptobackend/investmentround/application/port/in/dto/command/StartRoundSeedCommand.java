package ksh.tryptobackend.investmentround.application.port.in.dto.command;

import java.math.BigDecimal;

public record StartRoundSeedCommand(
    Long exchangeId,
    BigDecimal amount
) {
}
