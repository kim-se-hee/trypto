package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;

import java.util.Optional;

public interface RegretReportPersistencePort {

    Optional<RegretReport> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    RegretReport getByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    RegretReport save(RegretReport report);
}
