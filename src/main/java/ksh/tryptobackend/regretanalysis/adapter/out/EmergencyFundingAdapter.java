package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.EmergencyFundingQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.EmergencyFundingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("regretEmergencyFundingAdapter")
@RequiredArgsConstructor
public class EmergencyFundingAdapter implements EmergencyFundingPort {

    private final EmergencyFundingQueryPort emergencyFundingQueryPort;

    @Override
    public BigDecimal getTotalFundingAmount(Long roundId) {
        return emergencyFundingQueryPort.sumAmountByRoundId(roundId);
    }
}
