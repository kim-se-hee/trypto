package ksh.tryptobackend.marketdata.application.service;

import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.FindWithdrawalFeeUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.WithdrawalFeeResult;
import ksh.tryptobackend.marketdata.application.port.out.WithdrawalFeeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindWithdrawalFeeService implements FindWithdrawalFeeUseCase {

    private final WithdrawalFeeQueryPort withdrawalFeeQueryPort;

    @Override
    public Optional<WithdrawalFeeResult> findByExchangeIdAndCoinIdAndChain(
            Long exchangeId, Long coinId, String chain) {
        return withdrawalFeeQueryPort
                .findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
                .map(fee -> new WithdrawalFeeResult(fee.getFee(), fee.getMinWithdrawal()));
    }
}
