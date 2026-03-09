package ksh.tryptobackend.batch.regretreport;

import ksh.tryptobackend.regretanalysis.application.port.in.SaveRegretReportsUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GeneratedRegretReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@StepScope
@RequiredArgsConstructor
public class RegretReportItemWriter implements ItemWriter<GeneratedRegretReportResult> {

    private final SaveRegretReportsUseCase saveRegretReportsUseCase;

    @Override
    public void write(Chunk<? extends GeneratedRegretReportResult> chunk) {
        saveRegretReportsUseCase.saveAll(new ArrayList<>(chunk.getItems()));
    }
}
