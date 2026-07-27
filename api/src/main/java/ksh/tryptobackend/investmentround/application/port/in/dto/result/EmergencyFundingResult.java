package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmergencyFundingResult(Long fundingId, Long exchangeId, BigDecimal amount, LocalDateTime chargedAt) {}
