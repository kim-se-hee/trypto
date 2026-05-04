package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;

public interface RegretReportQueryPort {

    Optional<RegretReport> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    RegretReport getByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    boolean existsByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<ViolationDetail> findViolationDetailsByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
