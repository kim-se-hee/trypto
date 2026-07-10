package ksh.tryptobackend.regretanalysis.application.service;

import java.util.Map;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.WalletQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRegretReportService implements GetRegretReportUseCase {

    private final RegretReportQueryPort regretReportQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;
    private final WalletQueryPort walletQueryPort;

    @Override
    public RegretReportResult getRegretReport(GetRegretReportQuery query) {
        AnalysisRound round = investmentRoundQueryPort.getRound(query.roundId());
        round.validateOwnedBy(query.userId());

        if (!walletQueryPort.existsWallet(query.roundId(), query.exchangeId())) {
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }

        AnalysisExchange exchange = marketDataQueryPort.getExchange(query.exchangeId());
        AnalysisRules rules = investmentRoundQueryPort.findRules(query.roundId());
        RegretReport report =
                regretReportQueryPort.getByRoundIdAndExchangeId(
                        query.roundId(), query.exchangeId());
        Map<Long, String> coinSymbols =
                marketDataQueryPort.findCoinSymbols(report.getViolationDetails().extractCoinIds());

        return RegretReportResult.from(report, exchange, rules.toMap(), coinSymbols);
    }
}
