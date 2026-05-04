package ksh.tryptobackend.marketdata.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinChainJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.QExchangeCoinChainJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.QExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinChainQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeCoinChainQueryAdapter implements ExchangeCoinChainQueryPort {

    private static final QExchangeCoinChainJpaEntity ECC =
            QExchangeCoinChainJpaEntity.exchangeCoinChainJpaEntity;
    private static final QExchangeCoinJpaEntity EC = QExchangeCoinJpaEntity.exchangeCoinJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ExchangeCoinChain> findByExchangeIdAndCoinIdAndChain(
            Long exchangeId, Long coinId, String chain) {
        ExchangeCoinChainJpaEntity entity =
                queryFactory
                        .selectFrom(ECC)
                        .join(EC)
                        .on(ECC.exchangeCoinId.eq(EC.id))
                        .where(
                                EC.exchangeId.eq(exchangeId),
                                EC.coinId.eq(coinId),
                                ECC.chain.eq(chain))
                        .fetchOne();

        return Optional.ofNullable(entity).map(ExchangeCoinChainJpaEntity::toDomain);
    }

    @Override
    public List<ExchangeCoinChain> findByExchangeIdAndCoinId(Long exchangeId, Long coinId) {
        return queryFactory
                .selectFrom(ECC)
                .join(EC)
                .on(ECC.exchangeCoinId.eq(EC.id))
                .where(EC.exchangeId.eq(exchangeId), EC.coinId.eq(coinId))
                .fetch()
                .stream()
                .map(ExchangeCoinChainJpaEntity::toDomain)
                .toList();
    }
}
