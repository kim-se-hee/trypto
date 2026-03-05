package ksh.tryptobackend.ranking.application.port.in.dto.command;

import java.time.LocalDate;

public record CalculateRankingCommand(LocalDate snapshotDate) {
}
