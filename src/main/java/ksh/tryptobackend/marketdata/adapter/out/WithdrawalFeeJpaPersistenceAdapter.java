package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.repository.WithdrawalFeeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.WithdrawalFeeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.WithdrawalFeeInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WithdrawalFeeJpaPersistenceAdapter implements WithdrawalFeeQueryPort {

    private final WithdrawalFeeJpaRepository repository;

    @Override
    public Optional<WithdrawalFeeInfo> findByExchangeIdAndCoinIdAndChain(
        Long exchangeId, Long coinId, String chain) {
        return repository.findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
            .map(entity -> new WithdrawalFeeInfo(entity.getFee(), entity.getMinWithdrawal()));
    }
}
