package ksh.tryptobackend.marketdata.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinChainJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.QExchangeCoinChainJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.QExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinChainQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeCoinChainQueryAdapter implements ExchangeCoinChainQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ExchangeCoinChain> findByExchangeIdAndCoinIdAndChain(
        Long exchangeId, Long coinId, String chain) {
        QExchangeCoinChainJpaEntity ecc = QExchangeCoinChainJpaEntity.exchangeCoinChainJpaEntity;
        QExchangeCoinJpaEntity ec = QExchangeCoinJpaEntity.exchangeCoinJpaEntity;

        ExchangeCoinChainJpaEntity entity = queryFactory
            .selectFrom(ecc)
            .join(ec).on(ecc.exchangeCoinId.eq(ec.id))
            .where(
                ec.exchangeId.eq(exchangeId),
                ec.coinId.eq(coinId),
                ecc.chain.eq(chain)
            )
            .fetchOne();

        return Optional.ofNullable(entity)
            .map(ExchangeCoinChainJpaEntity::toDomain);
    }

    @Override
    public List<ExchangeCoinChain> findByExchangeIdAndCoinId(Long exchangeId, Long coinId) {
        QExchangeCoinChainJpaEntity ecc = QExchangeCoinChainJpaEntity.exchangeCoinChainJpaEntity;
        QExchangeCoinJpaEntity ec = QExchangeCoinJpaEntity.exchangeCoinJpaEntity;

        return queryFactory
            .selectFrom(ecc)
            .join(ec).on(ecc.exchangeCoinId.eq(ec.id))
            .where(
                ec.exchangeId.eq(exchangeId),
                ec.coinId.eq(coinId)
            )
            .fetch()
            .stream()
            .map(ExchangeCoinChainJpaEntity::toDomain)
            .toList();
    }
}
