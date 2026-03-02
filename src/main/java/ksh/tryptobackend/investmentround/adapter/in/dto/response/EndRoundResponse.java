package ksh.tryptobackend.investmentround.adapter.in.dto.response;

import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;

import java.time.LocalDateTime;

public record EndRoundResponse(Long roundId, RoundStatus status, LocalDateTime endedAt) {

    public static EndRoundResponse from(InvestmentRound round) {
        return new EndRoundResponse(round.getRoundId(), round.getStatus(), round.getEndedAt());
    }
}
