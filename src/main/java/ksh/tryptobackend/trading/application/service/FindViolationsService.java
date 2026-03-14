package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FindViolationsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;
import ksh.tryptobackend.trading.application.port.out.RuleViolationQueryPort;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindViolationsService implements FindViolationsUseCase {

    private final RuleViolationQueryPort ruleViolationQueryPort;
    private final FindWalletUseCase findWalletUseCase;

    @Override
    public List<ViolationResult> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId) {
        List<Long> walletIds = findWalletUseCase.findByExchangeId(exchangeId).stream()
            .map(WalletResult::walletId)
            .toList();

        return ruleViolationQueryPort.findByRuleIdsAndWalletIds(ruleIds, walletIds).stream()
            .map(ViolationResult::from)
            .toList();
    }
}
