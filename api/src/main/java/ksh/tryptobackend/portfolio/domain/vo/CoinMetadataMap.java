package ksh.tryptobackend.portfolio.domain.vo;

import java.util.Map;
import java.util.Objects;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public class CoinMetadataMap {

    private final Map<Long, CoinMetadata> values;

    public CoinMetadataMap(Map<Long, CoinMetadata> values) {
        this.values = Map.copyOf(values);
    }

    public boolean hasMetadata(Long coinId) {
        return values.containsKey(coinId);
    }

    public CoinMetadata getMetadata(Long coinId) {
        CoinMetadata metadata = values.get(coinId);
        if (metadata == null) {
            throw new CustomException(ErrorCode.COIN_NOT_FOUND);
        }
        return metadata;
    }

    public String getSymbol(Long coinId) {
        return getMetadata(coinId).symbol();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoinMetadataMap that = (CoinMetadataMap) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
