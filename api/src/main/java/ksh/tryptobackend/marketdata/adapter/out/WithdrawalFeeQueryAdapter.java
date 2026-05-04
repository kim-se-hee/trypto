package ksh.tryptobackend.marketdata.adapter.out;

import java.util.Optional;
import ksh.tryptobackend.marketdata.adapter.out.entity.WithdrawalFeeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.WithdrawalFeeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.WithdrawalFeeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.WithdrawalFee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WithdrawalFeeQueryAdapter implements WithdrawalFeeQueryPort {

    private final WithdrawalFeeJpaRepository repository;

    @Override
    public Optional<WithdrawalFee> findByExchangeIdAndCoinIdAndChain(
            Long exchangeId, Long coinId, String chain) {
        return repository
                .findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
                .map(WithdrawalFeeJpaEntity::toDomain);
    }
}
