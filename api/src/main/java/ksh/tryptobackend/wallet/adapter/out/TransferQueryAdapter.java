package ksh.tryptobackend.wallet.adapter.out;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.wallet.adapter.out.entity.QTransferJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.entity.TransferJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.TransferJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.TransferQueryPort;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.vo.TransferType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferQueryAdapter implements TransferQueryPort {

    private final JPAQueryFactory queryFactory;
    private final TransferJpaRepository transferRepository;

    @Override
    public Optional<Transfer> findByIdempotencyKey(UUID idempotencyKey) {
        return transferRepository
                .findByIdempotencyKey(idempotencyKey)
                .map(TransferJpaEntity::toDomain);
    }

    @Override
    public List<Transfer> findByCursor(
            Long walletId, TransferType type, Long cursorTransferId, int size) {
        QTransferJpaEntity transfer = QTransferJpaEntity.transferJpaEntity;

        return queryFactory
                .selectFrom(transfer)
                .where(
                        walletCondition(transfer, walletId, type),
                        cursorLt(transfer, cursorTransferId))
                .orderBy(transfer.id.desc())
                .limit(size)
                .fetch()
                .stream()
                .map(TransferJpaEntity::toDomain)
                .toList();
    }

    private BooleanExpression walletCondition(
            QTransferJpaEntity transfer, Long walletId, TransferType type) {
        return switch (type) {
            case DEPOSIT -> transfer.toWalletId.eq(walletId);
            case WITHDRAW -> transfer.fromWalletId.eq(walletId);
            case ALL -> transfer.fromWalletId.eq(walletId).or(transfer.toWalletId.eq(walletId));
        };
    }

    private BooleanExpression cursorLt(QTransferJpaEntity transfer, Long cursorTransferId) {
        return cursorTransferId != null ? transfer.id.lt(cursorTransferId) : null;
    }
}
