package ksh.tryptobackend.marketdata.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCandlesUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;
import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindCandlesService implements FindCandlesUseCase {

    private final CandleQueryPort candleQueryPort;
    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public List<Candle> findCandles(FindCandlesQuery query) {
        CandleFilter.validateIdentifiers(query.exchange(), query.coin());
        ExchangeSummary exchange = exchangeQueryPort
                .findExchangeSummaryByName(query.exchange())
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        CandleFilter filter = CandleFilter.of(
                query.exchange(),
                query.coin(),
                query.interval(),
                query.limit(),
                query.cursor(),
                exchange.baseCurrencySymbol());
        List<Candle> candles = candleQueryPort.findByFilter(filter);

        // 최신 조회(커서 없음)일 때만 아직 닫히지 않은 현재 구간 캔들을 맨 끝에 덧붙인다.
        // 과거 스크롤(커서 있음)에는 이미 닫힌 봉만 필요하므로 손대지 않는다.
        if (filter.cursor() == null) {
            return appendInProgress(candles, candleQueryPort.findInProgressCandle(filter));
        }
        return candles;
    }

    private List<Candle> appendInProgress(List<Candle> closed, Optional<Candle> inProgress) {
        if (inProgress.isEmpty()) {
            return closed;
        }
        Candle candle = inProgress.get();
        List<Candle> result = new ArrayList<>(closed);
        int lastIndex = result.size() - 1;
        // 집계 Task 가 이미 같은 구간을 써 둔 경우(구간 마감 직후)에는 진행봉으로 교체한다.
        if (lastIndex >= 0 && result.get(lastIndex).time().equals(candle.time())) {
            result.set(lastIndex, candle);
        } else {
            result.add(candle);
        }
        return result;
    }
}
