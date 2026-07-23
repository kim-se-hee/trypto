package ksh.tryptobackend.marketdata.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;

public interface CandleQueryPort {

    List<Candle> findByFilter(CandleFilter filter);

    /**
     * 아직 닫히지 않은 현재 구간의 캔들을 즉석 집계해 반환한다. 완성된 하위 구간은 저장된 상위봉으로, 남은 조각은 더 잘게
     * 채운다. 해당 구간에 데이터가 없으면 빈 값을 반환한다.
     */
    Optional<Candle> findInProgressCandle(CandleFilter filter);
}
