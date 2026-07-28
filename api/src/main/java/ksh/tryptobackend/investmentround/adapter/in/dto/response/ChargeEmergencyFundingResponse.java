package ksh.tryptobackend.investmentround.adapter.in.dto.response;

import java.math.BigDecimal;

public record ChargeEmergencyFundingResponse(
        Long roundId,
        Long exchangeId,
        BigDecimal chargedAmount,
        BigDecimal krwConvertedAmount,
        int remainingChargeCount) {

    public static ChargeEmergencyFundingResponse of(
            Long roundId,
            Long exchangeId,
            BigDecimal chargedAmount,
            BigDecimal krwConvertedAmount,
            int remainingChargeCount) {
        return new ChargeEmergencyFundingResponse(
                roundId, exchangeId, chargedAmount, krwConvertedAmount, remainingChargeCount);
    }
}
