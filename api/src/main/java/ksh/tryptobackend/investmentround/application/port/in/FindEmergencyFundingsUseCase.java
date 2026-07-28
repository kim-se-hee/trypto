package ksh.tryptobackend.investmentround.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.EmergencyFundingResult;

public interface FindEmergencyFundingsUseCase {

    List<EmergencyFundingResult> findByRoundId(Long roundId);

    Optional<EmergencyFundingResult> findByIdempotencyKey(UUID idempotencyKey);
}
