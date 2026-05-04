package ksh.tryptobackend.investmentround.application.service;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.FindActiveRoundsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ActiveRoundResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindActiveRoundsService implements FindActiveRoundsUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    public List<ActiveRoundResult> findAllActiveRounds() {
        return investmentRoundQueryPort.findAllActiveRounds().stream()
                .map(ActiveRoundResult::from)
                .toList();
    }
}
