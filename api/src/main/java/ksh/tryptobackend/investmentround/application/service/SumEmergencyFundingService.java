package ksh.tryptobackend.investmentround.application.service;

import java.math.BigDecimal;
import ksh.tryptobackend.investmentround.application.port.in.SumEmergencyFundingUseCase;
import ksh.tryptobackend.investmentround.application.port.out.EmergencyFundingQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SumEmergencyFundingService implements SumEmergencyFundingUseCase {

    private final EmergencyFundingQueryPort emergencyFundingQueryPort;

    @Override
    public BigDecimal sumByRoundId(Long roundId) {
        return emergencyFundingQueryPort.sumAmountByRoundId(roundId);
    }

    @Override
    public BigDecimal sumByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return emergencyFundingQueryPort.sumAmountByRoundIdAndExchangeId(roundId, exchangeId);
    }
}
