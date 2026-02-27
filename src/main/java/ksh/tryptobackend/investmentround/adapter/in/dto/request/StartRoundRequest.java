package ksh.tryptobackend.investmentround.adapter.in.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundRuleCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundSeedCommand;
import ksh.tryptobackend.trading.domain.vo.RuleType;

import java.math.BigDecimal;
import java.util.List;

public record StartRoundRequest(
    @NotNull Long userId,
    @NotEmpty List<@Valid SeedRequest> seeds,
    @NotNull @DecimalMin("0") BigDecimal emergencyFundingLimit,
    List<@Valid RuleRequest> rules
) {

    public StartRoundCommand toCommand() {
        List<StartRoundSeedCommand> seedCommands = seeds.stream()
            .map(seed -> new StartRoundSeedCommand(seed.exchangeId(), seed.amount()))
            .toList();
        List<StartRoundRuleCommand> ruleCommands = rules == null
            ? List.of()
            : rules.stream().map(rule -> new StartRoundRuleCommand(rule.ruleType(), rule.thresholdValue())).toList();
        return new StartRoundCommand(userId, seedCommands, emergencyFundingLimit, ruleCommands);
    }

    public record SeedRequest(
        @NotNull Long exchangeId,
        @NotNull @DecimalMin("0") BigDecimal amount
    ) {
    }

    public record RuleRequest(
        @NotNull RuleType ruleType,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal thresholdValue
    ) {
    }
}
