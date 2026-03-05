package ksh.tryptobackend.ranking.application.port.in;

import ksh.tryptobackend.ranking.application.port.in.dto.command.CalculateRankingCommand;

public interface CalculateRankingUseCase {

    void calculateRanking(CalculateRankingCommand command);
}
