package ksh.tryptobackend.investmentround.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.EndRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.EndRoundCommand;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundCommandPort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EndRoundService implements EndRoundUseCase {

    private final InvestmentRoundCommandPort investmentRoundCommandPort;
    private final Clock clock;

    @Override
    @Transactional
    public InvestmentRound endRound(EndRoundCommand command) {
        InvestmentRound round = getRound(command.roundId());
        round.validateOwnedBy(command.userId());

        if (round.isEnded()) {
            return round;
        }

        round.end(LocalDateTime.now(clock));
        return investmentRoundCommandPort.save(round);
    }

    private InvestmentRound getRound(Long roundId) {
        return investmentRoundCommandPort
                .findById(roundId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
    }
}
