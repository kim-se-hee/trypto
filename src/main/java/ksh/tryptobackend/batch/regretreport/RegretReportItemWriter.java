package ksh.tryptobackend.batch.regretreport;

import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportCommandPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@StepScope
@RequiredArgsConstructor
public class RegretReportItemWriter implements ItemWriter<RegretReport> {

    private final RegretReportCommandPort regretReportCommandPort;

    @Override
    public void write(Chunk<? extends RegretReport> chunk) {
        regretReportCommandPort.saveAll(new ArrayList<>(chunk.getItems()));
    }
}
