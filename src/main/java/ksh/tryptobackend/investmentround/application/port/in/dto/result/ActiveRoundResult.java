package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;

import java.time.LocalDateTime;

public record ActiveRoundResult(Long roundId, Long userId, LocalDateTime startedAt) {

    public static ActiveRoundResult from(RoundOverview info) {
        return new ActiveRoundResult(info.roundId(), info.userId(), info.startedAt());
    }
}
