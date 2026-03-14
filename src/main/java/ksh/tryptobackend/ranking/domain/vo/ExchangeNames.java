package ksh.tryptobackend.ranking.domain.vo;

import java.util.Map;
import java.util.Objects;

public class ExchangeNames {

    private final Map<Long, String> nameByExchangeId;

    public ExchangeNames(Map<Long, String> nameByExchangeId) {
        this.nameByExchangeId = Map.copyOf(nameByExchangeId);
    }

    public String getName(Long exchangeId) {
        return nameByExchangeId.getOrDefault(exchangeId, "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeNames that = (ExchangeNames) o;
        return Objects.equals(nameByExchangeId, that.nameByExchangeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameByExchangeId);
    }
}
