package ksh.tryptoengine.wal;

import java.util.List;
import ksh.tryptoengine.matching.OrderDetail;

public record Snapshot(long lastSeq, List<PairSnapshot> pairs) {
    public record PairSnapshot(Long exchangeCoinId, List<OrderDetail> orders) {}
}
