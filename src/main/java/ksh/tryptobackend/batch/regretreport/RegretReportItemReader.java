package ksh.tryptobackend.batch.regretreport;

import ksh.tryptobackend.regretanalysis.application.port.out.ActiveRoundExchangePort;
import ksh.tryptobackend.regretanalysis.domain.vo.ActiveRoundExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class RegretReportItemReader implements ItemReader<RegretReportInput> {

    private final ActiveRoundExchangePort activeRoundExchangePort;

    private Iterator<RegretReportInput> iterator;

    @Override
    public RegretReportInput read() {
        if (iterator == null) {
            iterator = buildInputList().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<RegretReportInput> buildInputList() {
        return activeRoundExchangePort.findAllActiveRoundExchanges().stream()
            .map(this::toInput)
            .toList();
    }

    private RegretReportInput toInput(ActiveRoundExchange info) {
        return new RegretReportInput(
            info.roundId(), info.userId(), info.exchangeId(),
            info.walletId(), info.startedAt()
        );
    }
}
