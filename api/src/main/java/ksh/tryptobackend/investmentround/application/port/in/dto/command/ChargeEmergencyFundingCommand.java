package ksh.tryptobackend.investmentround.application.port.in.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeEmergencyFundingCommand(Long roundId, Long userId, BigDecimal amount, UUID idempotencyKey) {}
