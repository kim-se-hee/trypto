package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.RoundInfo;

import java.util.Optional;

public interface InvestmentRoundPort {

    Optional<RoundInfo> findActiveRoundByUserId(Long userId);
}
