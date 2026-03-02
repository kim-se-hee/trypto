package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;

import java.util.List;
import java.util.Optional;

public interface RegretReportPersistencePort {

    Optional<RegretReport> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    RegretReport getByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    boolean existsByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<ViolationDetail> findViolationDetailsByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    RegretReport save(RegretReport report);
}
