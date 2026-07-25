package ksh.tryptobackend.regretanalysis.adapter.out;

import java.util.List;
import ksh.tryptobackend.regretanalysis.adapter.out.persistence.entity.RegretReportJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.persistence.repository.RegretReportJpaRepository;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportCommandPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegretReportCommandAdapter implements RegretReportCommandPort {

    private final RegretReportJpaRepository repository;

    @Override
    public void saveAll(List<RegretReport> reports) {
        reports.forEach(this::saveOrUpdate);
    }

    /**
     * 같은 (라운드, 거래소) 리포트는 1건만 유지한다. 기존 행이 있으면 내용만 갱신하므로 배치가 매일 돌아도 행이 쌓이지 않는다. 갱신은 영속성 컨텍스트의 변경 감지로
     * 반영되므로 별도의 저장 호출이 필요 없다.
     */
    private void saveOrUpdate(RegretReport report) {
        repository
                .findByRoundIdAndExchangeId(report.getRoundId(), report.getExchangeId())
                .ifPresentOrElse(
                        existing -> existing.updateFrom(report),
                        () -> repository.save(RegretReportJpaEntity.fromDomain(report)));
    }
}
