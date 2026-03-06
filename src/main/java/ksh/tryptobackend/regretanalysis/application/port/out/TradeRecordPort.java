package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.vo.TradeRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRecordPort {

    List<TradeRecord> findByOrderIds(List<Long> orderIds);

    List<TradeRecord> findSellOrdersAfter(Long walletId, Long exchangeCoinId, LocalDateTime after);
}
