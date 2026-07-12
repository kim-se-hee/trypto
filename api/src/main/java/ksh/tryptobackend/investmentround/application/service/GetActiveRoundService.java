package ksh.tryptobackend.investmentround.application.service;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.GetActiveRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.GetActiveRoundQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.GetActiveRoundResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.RuleQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.WalletQueryPort;
import ksh.tryptobackend.investmentround.domain.model.Rule;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;
import ksh.tryptobackend.investmentround.domain.vo.RoundWallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetActiveRoundService implements GetActiveRoundUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final RuleQueryPort ruleQueryPort;
    private final WalletQueryPort walletQueryPort;

    @Override
    @Transactional(readOnly = true)
    public GetActiveRoundResult getActiveRound(GetActiveRoundQuery query) {
        RoundOverview round = investmentRoundQueryPort
                .findActiveRoundByUserId(query.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_ACTIVE));
        List<Rule> rules = ruleQueryPort.findByRoundId(round.roundId());
        List<RoundWallet> wallets = walletQueryPort.findWalletsByRoundId(round.roundId());

        return GetActiveRoundResult.from(round, rules, wallets);
    }
}
