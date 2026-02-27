package ksh.tryptobackend.investmentround.application.port.in;

import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundResult;

public interface StartRoundUseCase {

    StartRoundResult startRound(StartRoundCommand command);
}
