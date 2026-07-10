package ksh.tryptobackend.investmentround.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.investmentround.application.port.in.EndRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.EndRoundCommand;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundCommandPort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EndRoundService implements EndRoundUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final InvestmentRoundCommandPort investmentRoundCommandPort;
    private final Clock clock;

    @Override
    @Transactional
    public InvestmentRound endRound(EndRoundCommand command) {
        InvestmentRound round = investmentRoundQueryPort.getById(command.roundId());
        round.validateOwnedBy(command.userId());

        if (round.isEnded()) {
            return round;
        }

        round.end(LocalDateTime.now(clock));
        return investmentRoundCommandPort.save(round);
    }
}
