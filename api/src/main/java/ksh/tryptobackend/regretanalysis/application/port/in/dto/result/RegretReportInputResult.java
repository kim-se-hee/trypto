package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import java.time.LocalDateTime;
import ksh.tryptobackend.regretanalysis.domain.vo.ActiveRoundExchange;

public record RegretReportInputResult(
        Long roundId, Long userId, Long exchangeId, Long walletId, LocalDateTime startedAt) {

    public static RegretReportInputResult from(ActiveRoundExchange activeRoundExchange) {
        return new RegretReportInputResult(
                activeRoundExchange.roundId(),
                activeRoundExchange.userId(),
                activeRoundExchange.exchangeId(),
                activeRoundExchange.walletId(),
                activeRoundExchange.startedAt());
    }
}
