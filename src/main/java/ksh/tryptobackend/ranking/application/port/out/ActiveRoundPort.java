package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.ActiveRound;

import java.util.Optional;

public interface ActiveRoundPort {

    Optional<ActiveRound> findActiveRoundByUserId(Long userId);
}
