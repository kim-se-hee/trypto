package ksh.tryptobackend.wallet.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.adapter.out.entity.QWalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletCommandPort;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WalletCommandAdapter implements WalletCommandPort {

    private final WalletJpaRepository walletRepository;
    private final WalletBalanceJpaRepository balanceRepository;
    private final JPAQueryFactory queryFactory;

    private static final QWalletBalanceJpaEntity balance =
            QWalletBalanceJpaEntity.walletBalanceJpaEntity;

    @Override
    public Long createWallet(
            Long roundId, Long exchangeId, BigDecimal seedAmount, LocalDateTime createdAt) {
        Wallet wallet = Wallet.create(roundId, exchangeId, seedAmount, createdAt);
        WalletJpaEntity saved = walletRepository.save(WalletJpaEntity.fromDomain(wallet));
        return saved.getId();
    }

    @Override
    public Long createWalletWithBalance(
            Long roundId,
            Long exchangeId,
            Long baseCurrencyCoinId,
            BigDecimal initialAmount,
            LocalDateTime createdAt) {
        Long walletId = createWallet(roundId, exchangeId, initialAmount, createdAt);
        balanceRepository.save(
                new WalletBalanceJpaEntity(
                        walletId, baseCurrencyCoinId, initialAmount, BigDecimal.ZERO));
        return walletId;
    }

    @Override
    @Transactional
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        long count =
                queryFactory
                        .update(balance)
                        .set(balance.available, balance.available.subtract(amount))
                        .where(
                                balance.walletId
                                        .eq(walletId)
                                        .and(balance.coinId.eq(coinId))
                                        .and(balance.available.goe(amount)))
                        .execute();
        if (count == 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    @Override
    @Transactional
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        long count =
                queryFactory
                        .update(balance)
                        .set(balance.available, balance.available.add(amount))
                        .where(balance.walletId.eq(walletId).and(balance.coinId.eq(coinId)))
                        .execute();
        if (count == 0) {
            createBalanceWithAvailable(walletId, coinId, amount);
        }
    }

    @Override
    @Transactional
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        long count =
                queryFactory
                        .update(balance)
                        .set(balance.available, balance.available.subtract(amount))
                        .set(balance.locked, balance.locked.add(amount))
                        .where(
                                balance.walletId
                                        .eq(walletId)
                                        .and(balance.coinId.eq(coinId))
                                        .and(balance.available.goe(amount)))
                        .execute();
        if (count == 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    @Override
    @Transactional
    public void unlockBalance(Long walletId, Long coinId, BigDecimal amount) {
        queryFactory
                .update(balance)
                .set(balance.locked, balance.locked.subtract(amount))
                .set(balance.available, balance.available.add(amount))
                .where(balance.walletId.eq(walletId).and(balance.coinId.eq(coinId)))
                .execute();
    }

    private void createBalanceWithAvailable(Long walletId, Long coinId, BigDecimal amount) {
        try {
            balanceRepository.saveAndFlush(
                    new WalletBalanceJpaEntity(walletId, coinId, amount, BigDecimal.ZERO));
        } catch (DataIntegrityViolationException e) {
            long count =
                    queryFactory
                            .update(balance)
                            .set(balance.available, balance.available.add(amount))
                            .where(balance.walletId.eq(walletId).and(balance.coinId.eq(coinId)))
                            .execute();
            if (count == 0) {
                throw e;
            }
        }
    }
}
