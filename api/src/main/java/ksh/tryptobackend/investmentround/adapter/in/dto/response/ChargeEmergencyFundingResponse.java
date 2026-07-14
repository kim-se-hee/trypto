package ksh.tryptobackend.investmentround.adapter.in.dto.response;

import java.math.BigDecimal;

public record ChargeEmergencyFundingResponse(Long roundId, BigDecimal chargedAmount, int remainingChargeCount) {

    public static ChargeEmergencyFundingResponse of(Long roundId, BigDecimal chargedAmount, int remainingChargeCount) {
        return new ChargeEmergencyFundingResponse(roundId, chargedAmount, remainingChargeCount);
    }
}
