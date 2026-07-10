package ksh.tryptobackend.regretanalysis.application.service;

import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.in.FindRegretReportInputsUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportInputResult;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.WalletQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindRegretReportInputsService implements FindRegretReportInputsUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final WalletQueryPort walletQueryPort;

    @Override
    public List<RegretReportInputResult> findAllInputs() {
        return investmentRoundQueryPort.findActiveRounds().stream()
                .flatMap(
                        round ->
                                walletQueryPort.findWallets(round.roundId()).stream()
                                        .map(round::combineWith))
                .map(RegretReportInputResult::from)
                .toList();
    }
}
