package ksh.tryptobackend.marketdata.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinChainJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.QExchangeCoinChainJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.QExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinChainQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeCoinChainInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeCoinChainJpaPersistenceAdapter implements ExchangeCoinChainQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ExchangeCoinChainInfo> findByExchangeIdAndCoinIdAndChain(
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
            .map(e -> new ExchangeCoinChainInfo(
                e.getId(), e.getExchangeCoinId(), e.getChain(), e.isTagRequired()));
    }
}
