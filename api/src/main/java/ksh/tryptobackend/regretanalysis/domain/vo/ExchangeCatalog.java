package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/** 라운드에 등장하는 거래소들. 거래소마다 기축통화가 달라 원화로 합치려면 어느 거래소의 금액인지 알아야 한다. */
public final class ExchangeCatalog {

    private final Map<Long, AnalysisExchange> exchangeById;

    private ExchangeCatalog(Map<Long, AnalysisExchange> exchangeById) {
        this.exchangeById = Map.copyOf(exchangeById);
    }

    public static ExchangeCatalog of(Map<Long, AnalysisExchange> exchangeById) {
        return new ExchangeCatalog(exchangeById);
    }

    public AnalysisExchange get(Long exchangeId) {
        return exchangeById.get(exchangeId);
    }

    public BigDecimal toKrw(Long exchangeId, BigDecimal amount) {
        return get(exchangeId).toKrw(amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExchangeCatalog that)) return false;
        return Objects.equals(exchangeById, that.exchangeById);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchangeById);
    }
}
