package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.EndRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.EndRoundCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.EndRoundResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundPersistencePort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EndRoundService implements EndRoundUseCase {

    private final InvestmentRoundPersistencePort investmentRoundPersistencePort;
    private final Clock clock;

    @Override
    @Transactional
    public EndRoundResult endRound(EndRoundCommand command) {
        InvestmentRound round = getRound(command.roundId());
        round.validateOwnedBy(command.userId());

        InvestmentRound endedRound = round.end(LocalDateTime.now(clock));
        InvestmentRound savedRound = saveIfChanged(round, endedRound);

        return toResult(savedRound);
    }

    private InvestmentRound getRound(Long roundId) {
        return investmentRoundPersistencePort.findById(roundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
    }

    private InvestmentRound saveIfChanged(InvestmentRound original, InvestmentRound ended) {
        if (original == ended) {
            return original;
        }
        return investmentRoundPersistencePort.save(ended);
    }

    private EndRoundResult toResult(InvestmentRound round) {
        return new EndRoundResult(round.getRoundId(), round.getStatus(), round.getEndedAt());
    }
}
