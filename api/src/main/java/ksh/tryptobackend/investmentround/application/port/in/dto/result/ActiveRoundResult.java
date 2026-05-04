package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.time.LocalDateTime;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;

public record ActiveRoundResult(Long roundId, Long userId, LocalDateTime startedAt) {

    public static ActiveRoundResult from(RoundOverview info) {
        return new ActiveRoundResult(info.roundId(), info.userId(), info.startedAt());
    }
}
