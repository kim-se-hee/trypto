package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;

public interface RegretReportQueryPort {

    Optional<RegretReport> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    RegretReport getByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
