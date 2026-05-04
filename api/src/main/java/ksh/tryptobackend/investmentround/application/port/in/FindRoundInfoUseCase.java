package ksh.tryptobackend.investmentround.application.port.in;

import java.util.Optional;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;

public interface FindRoundInfoUseCase {

    Optional<RoundInfoResult> findById(Long roundId);

    Optional<RoundInfoResult> findActiveByUserId(Long userId);
}
