package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChargeEmergencyFundingResult(
        Long roundId,
        Long exchangeId,
        BigDecimal chargedAmount,
        int remainingChargeCount,
        LocalDateTime chargedAt) {}
