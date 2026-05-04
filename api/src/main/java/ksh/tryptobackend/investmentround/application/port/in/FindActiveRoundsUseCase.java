package ksh.tryptobackend.investmentround.application.port.in;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ActiveRoundResult;

public interface FindActiveRoundsUseCase {

    List<ActiveRoundResult> findAllActiveRounds();
}
