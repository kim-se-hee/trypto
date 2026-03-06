package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.AnalysisExchangeProfilePort;
import ksh.tryptobackend.regretanalysis.application.port.out.AnalysisRoundPort;
import ksh.tryptobackend.regretanalysis.application.port.out.AnalysisRulePort;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportPersistencePort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchangeProfile;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRule;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetRegretReportService implements GetRegretReportUseCase {

    private final AnalysisRoundPort analysisRoundPort;
    private final RegretReportPersistencePort regretReportPersistencePort;
    private final AnalysisRulePort analysisRulePort;
    private final AnalysisExchangeProfilePort analysisExchangeProfilePort;
    private final CoinQueryPort coinQueryPort;

    @Override
    public RegretReportResult getRegretReport(GetRegretReportQuery query) {
        validateRoundOwner(query.roundId(), query.userId());
        validateWalletExistsForExchange(query.roundId(), query.exchangeId());
        AnalysisExchangeProfile exchange = analysisExchangeProfilePort.getExchangeProfile(query.exchangeId());
        AnalysisRules rules = analysisRulePort.findByRoundId(query.roundId());

        RegretReport report = regretReportPersistencePort.getByRoundIdAndExchangeId(
            query.roundId(), query.exchangeId());

        return toResult(report, exchange, rules);
    }

    private void validateRoundOwner(Long roundId, Long userId) {
        AnalysisRound round = analysisRoundPort.getRound(roundId);
        if (!round.userId().equals(userId)) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
    }

    private void validateWalletExistsForExchange(Long roundId, Long exchangeId) {
        if (!analysisExchangeProfilePort.existsWalletForExchange(roundId, exchangeId)) {
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }
    }

    private RegretReportResult toResult(RegretReport report, AnalysisExchangeProfile exchange,
                                        AnalysisRules rules) {
        Map<Long, AnalysisRule> ruleMap = rules.toMap();
        Map<Long, String> coinSymbols = coinQueryPort.findSymbolsByIds(
            report.getViolationDetails().extractCoinIds());

        return RegretReportResult.from(report, exchange, ruleMap, coinSymbols);
    }
}
