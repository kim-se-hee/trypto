package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;

import java.time.LocalDateTime;

public record ActiveRoundResult(Long roundId, Long userId, LocalDateTime startedAt) {

    public static ActiveRoundResult from(InvestmentRoundInfo info) {
        return new ActiveRoundResult(info.roundId(), info.userId(), info.startedAt());
    }
}
