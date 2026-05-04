package ksh.tryptobackend.regretanalysis.application.service;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.FindActiveRoundsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ActiveRoundResult;
import ksh.tryptobackend.regretanalysis.application.port.in.FindRegretReportInputsUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportInputResult;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindRegretReportInputsService implements FindRegretReportInputsUseCase {

    private final FindActiveRoundsUseCase findActiveRoundsUseCase;
    private final FindWalletUseCase findWalletUseCase;

    @Override
    public List<RegretReportInputResult> findAllInputs() {
        List<ActiveRoundResult> activeRounds = findActiveRoundsUseCase.findAllActiveRounds();
        return activeRounds.stream().flatMap(round -> findWalletsForRound(round).stream()).toList();
    }

    private List<RegretReportInputResult> findWalletsForRound(ActiveRoundResult round) {
        return findWalletUseCase.findByRoundId(round.roundId()).stream()
                .map(wallet -> toResult(round, wallet))
                .toList();
    }

    private RegretReportInputResult toResult(ActiveRoundResult round, WalletResult wallet) {
        return new RegretReportInputResult(
                round.roundId(),
                round.userId(),
                wallet.exchangeId(),
                wallet.walletId(),
                round.startedAt());
    }
}
