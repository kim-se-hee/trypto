package ksh.tryptobackend.transfer.adapter.out;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import ksh.tryptobackend.transfer.adapter.out.entity.QTransferJpaEntity;
import ksh.tryptobackend.transfer.adapter.out.entity.TransferJpaEntity;
import ksh.tryptobackend.transfer.application.port.out.TransferQueryPort;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferQueryAdapter implements TransferQueryPort {

    private final JPAQueryFactory queryFactory;

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
