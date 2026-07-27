package ksh.tryptobackend.investmentround.application.service;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.FindEmergencyFundingsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.EmergencyFundingResult;
import ksh.tryptobackend.investmentround.application.port.out.EmergencyFundingQueryPort;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindEmergencyFundingsService implements FindEmergencyFundingsUseCase {

    private final EmergencyFundingQueryPort emergencyFundingQueryPort;

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyFundingResult> findByRoundId(Long roundId) {
        return emergencyFundingQueryPort.findAllByRoundId(roundId).stream()
                .map(this::toResult)
                .toList();
    }

    private EmergencyFundingResult toResult(EmergencyFunding funding) {
        return new EmergencyFundingResult(funding.id(), funding.exchangeId(), funding.amount(), funding.createdAt());
    }
}
