package ksh.tryptobackend.portfolio.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

import java.util.Map;
import java.util.Objects;

public class CoinSnapshotMap {

    private final Map<Long, CoinSnapshot> values;

    public CoinSnapshotMap(Map<Long, CoinSnapshot> values) {
        this.values = Map.copyOf(values);
    }

    public CoinSnapshot getCoinSnapshot(Long coinId) {
        CoinSnapshot snapshot = values.get(coinId);
        if (snapshot == null) {
            throw new CustomException(ErrorCode.COIN_NOT_FOUND);
        }
        return snapshot;
    }

    public String getSymbol(Long coinId) {
        return getCoinSnapshot(coinId).symbol();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoinSnapshotMap that = (CoinSnapshotMap) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
