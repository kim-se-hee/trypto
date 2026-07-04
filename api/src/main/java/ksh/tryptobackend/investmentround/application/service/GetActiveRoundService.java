package ksh.tryptobackend.investmentround.application.service;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.GetActiveRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.GetActiveRoundQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.GetActiveRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.GetActiveRoundWalletResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.RuleQueryPort;
import ksh.tryptobackend.investmentround.domain.model.Rule;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetActiveRoundService implements GetActiveRoundUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final RuleQueryPort ruleQueryPort;

    private final FindWalletUseCase findWalletUseCase;

    @Override
    @Transactional(readOnly = true)
    public GetActiveRoundResult getActiveRound(GetActiveRoundQuery query) {
        RoundOverview round = getActiveRound(query.userId());
        List<Rule> rules = ruleQueryPort.findByRoundId(round.roundId());
        List<GetActiveRoundWalletResult> wallets = toWalletResults(round.roundId());

        return GetActiveRoundResult.from(round, rules, wallets);
    }

    private RoundOverview getActiveRound(Long userId) {
        return investmentRoundQueryPort
                .findActiveRoundByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_ACTIVE));
    }

    private List<GetActiveRoundWalletResult> toWalletResults(Long roundId) {
        return findWalletUseCase.findByRoundId(roundId).stream().map(this::toWalletResult).toList();
    }

    private GetActiveRoundWalletResult toWalletResult(WalletResult walletResult) {
        return new GetActiveRoundWalletResult(walletResult.walletId(), walletResult.exchangeId());
    }
}
