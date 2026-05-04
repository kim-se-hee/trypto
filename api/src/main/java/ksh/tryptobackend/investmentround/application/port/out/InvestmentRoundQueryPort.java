package ksh.tryptobackend.investmentround.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;

public interface InvestmentRoundQueryPort {

    Optional<RoundOverview> findActiveRoundByUserId(Long userId);

    Optional<RoundOverview> findRoundInfoById(Long roundId);

    List<RoundOverview> findAllActiveRounds();
}
