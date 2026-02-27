package ksh.tryptobackend.investmentround.application.port.in.dto.command;

import java.math.BigDecimal;
import java.util.List;

public record StartRoundCommand(
    Long userId,
    List<StartRoundSeedCommand> seeds,
    BigDecimal emergencyFundingLimit,
    List<StartRoundRuleCommand> rules
) {
}
