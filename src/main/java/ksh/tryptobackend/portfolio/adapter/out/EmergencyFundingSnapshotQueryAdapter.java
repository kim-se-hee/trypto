package ksh.tryptobackend.portfolio.adapter.out;

import ksh.tryptobackend.investmentround.application.port.in.SumEmergencyFundingUseCase;
import ksh.tryptobackend.portfolio.application.port.out.EmergencyFundingSnapshotQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class EmergencyFundingSnapshotQueryAdapter implements EmergencyFundingSnapshotQueryPort {

    private final SumEmergencyFundingUseCase sumEmergencyFundingUseCase;

    @Override
    public BigDecimal sumByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return sumEmergencyFundingUseCase.sumByRoundIdAndExchangeId(roundId, exchangeId);
    }
}
