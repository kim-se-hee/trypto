package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.regretanalysis.application.port.in.FindRegretReportInputsUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportInputResult;
import ksh.tryptobackend.regretanalysis.application.port.out.ActiveRoundExchangeQueryPort;
import ksh.tryptobackend.regretanalysis.domain.vo.ActiveRoundExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindRegretReportInputsService implements FindRegretReportInputsUseCase {

    private final ActiveRoundExchangeQueryPort activeRoundExchangeQueryPort;

    @Override
    public List<RegretReportInputResult> findAllInputs() {
        return activeRoundExchangeQueryPort.findAllActiveRoundExchanges().stream()
            .map(this::toResult)
            .toList();
    }

    private RegretReportInputResult toResult(ActiveRoundExchange exchange) {
        return new RegretReportInputResult(
            exchange.roundId(), exchange.userId(), exchange.exchangeId(),
            exchange.walletId(), exchange.startedAt());
    }
}
