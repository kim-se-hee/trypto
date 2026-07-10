package ksh.tryptobackend.wallet.adapter.out.acl;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.wallet.application.port.out.InvestmentRoundQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletAclInvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final FindRoundInfoUseCase findRoundInfoUseCase;

    @Override
    public Long getOwnerId(Long roundId) {
        return findRoundInfoUseCase
                .findById(roundId)
                .map(RoundInfoResult::userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
    }
}
