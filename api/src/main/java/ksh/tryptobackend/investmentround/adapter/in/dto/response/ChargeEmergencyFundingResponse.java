package ksh.tryptobackend.investmentround.adapter.in.dto.response;

import java.math.BigDecimal;

public record ChargeEmergencyFundingResponse(
        Long roundId, Long exchangeId, BigDecimal chargedAmount, int remainingChargeCount) {

    public static ChargeEmergencyFundingResponse of(
            Long roundId, Long exchangeId, BigDecimal chargedAmount, int remainingChargeCount) {
        return new ChargeEmergencyFundingResponse(
                roundId, exchangeId, chargedAmount, remainingChargeCount);
    }
}
