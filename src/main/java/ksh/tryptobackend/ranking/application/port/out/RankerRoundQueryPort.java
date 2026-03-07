package ksh.tryptobackend.ranking.application.port.out;

import java.util.Optional;

public interface RankerRoundQueryPort {

    Optional<Long> findActiveRoundIdByUserId(Long userId);
}
