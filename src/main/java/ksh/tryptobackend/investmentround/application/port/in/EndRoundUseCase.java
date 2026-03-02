package ksh.tryptobackend.investmentround.application.port.in;

import ksh.tryptobackend.investmentround.application.port.in.dto.command.EndRoundCommand;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;

public interface EndRoundUseCase {

    InvestmentRound endRound(EndRoundCommand command);
}
