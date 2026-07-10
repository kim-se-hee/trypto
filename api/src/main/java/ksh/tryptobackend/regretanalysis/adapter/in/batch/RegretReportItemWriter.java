package ksh.tryptobackend.regretanalysis.adapter.in.batch;

import java.util.ArrayList;
import ksh.tryptobackend.regretanalysis.application.port.in.SaveRegretReportsUseCase;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class RegretReportItemWriter implements ItemWriter<RegretReport> {

    private final SaveRegretReportsUseCase saveRegretReportsUseCase;

    @Override
    public void write(Chunk<? extends RegretReport> chunk) {
        saveRegretReportsUseCase.saveAll(new ArrayList<>(chunk.getItems()));
    }
}
