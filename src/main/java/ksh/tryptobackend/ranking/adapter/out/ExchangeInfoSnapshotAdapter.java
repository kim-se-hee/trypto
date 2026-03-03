package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeDetail;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import ksh.tryptobackend.ranking.application.port.out.ExchangeInfoQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.ExchangeSnapshotInfo;
import ksh.tryptobackend.ranking.domain.vo.KrwConversionRate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("rankingExchangeInfoSnapshotAdapter")
@RequiredArgsConstructor
public class ExchangeInfoSnapshotAdapter implements ExchangeInfoQueryPort {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public ExchangeSnapshotInfo getExchangeInfo(Long exchangeId) {
        ExchangeDetail detail = exchangeQueryPort.findExchangeDetailById(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        KrwConversionRate rate = toConversionRate(detail.marketType());
        return new ExchangeSnapshotInfo(exchangeId, detail.baseCurrencyCoinId(), rate);
    }

    private KrwConversionRate toConversionRate(ExchangeMarketType marketType) {
        return switch (marketType) {
            case DOMESTIC -> KrwConversionRate.DOMESTIC;
            case OVERSEAS -> KrwConversionRate.OVERSEAS;
        };
    }
}
