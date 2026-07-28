package ksh.tryptobackend.investmentround.adapter.in.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.ChargeEmergencyFundingCommand;

public record ChargeEmergencyFundingRequest(
        @NotNull Long exchangeId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @NotNull UUID idempotencyKey) {

    public ChargeEmergencyFundingCommand toCommand(Long roundId, Long userId) {
        return new ChargeEmergencyFundingCommand(roundId, userId, exchangeId, amount, idempotencyKey);
    }
}
