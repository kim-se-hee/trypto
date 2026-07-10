package ksh.tryptobackend.ranking.adapter.out.acl;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.ranking.application.port.out.InvestmentRoundQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("rankingAclInvestmentRoundQueryAdapter")
@RequiredArgsConstructor
public class AclInvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final FindRoundInfoUseCase findRoundInfoUseCase;

    @Override
    public Long getActiveRoundId(Long userId) {
        return findRoundInfoUseCase
                .findActiveByUserId(userId)
                .map(RoundInfoResult::roundId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_ACTIVE));
    }
}
