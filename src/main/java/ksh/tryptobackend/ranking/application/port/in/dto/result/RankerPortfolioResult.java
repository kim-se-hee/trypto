package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.math.BigDecimal;
import java.util.List;

public record RankerPortfolioResult(
    Long userId,
    String nickname,
    int rank,
    BigDecimal profitRate,
    List<PortfolioHoldingResult> holdings
) {
}
