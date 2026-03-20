package ksh.tryptobackend.wallet.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.wallet.adapter.out.entity.DepositAddressJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.entity.QDepositAddressJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.entity.QWalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.DepositAddressJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressQueryPort;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DepositAddressQueryAdapter implements DepositAddressQueryPort {

    private final DepositAddressJpaRepository repository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<DepositAddress> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId)
            .map(DepositAddressJpaEntity::toDomain);
    }

    @Override
    public Optional<DepositAddress> findByRoundIdAndAddress(Long roundId, String address) {
        QDepositAddressJpaEntity da = QDepositAddressJpaEntity.depositAddressJpaEntity;
        QWalletJpaEntity w = QWalletJpaEntity.walletJpaEntity;

        DepositAddressJpaEntity entity = queryFactory
            .selectFrom(da)
            .join(w).on(da.walletId.eq(w.id))
            .where(
                w.roundId.eq(roundId),
                da.address.eq(address)
            )
            .fetchOne();

        return Optional.ofNullable(entity)
            .map(DepositAddressJpaEntity::toDomain);
    }
}
