package ksh.tryptobackend.regretanalysis.application.service;

import java.util.Map;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindInvestmentRulesUseCase;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.InvestmentRuleResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinSymbolsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRoundStatus;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRule;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRegretReportService implements GetRegretReportUseCase {

    private final FindRoundInfoUseCase findRoundInfoUseCase;
    private final FindInvestmentRulesUseCase findInvestmentRulesUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindCoinSymbolsUseCase findCoinSymbolsUseCase;
    private final FindWalletUseCase findWalletUseCase;
    private final RegretReportQueryPort regretReportQueryPort;

    @Override
    public RegretReportResult getRegretReport(GetRegretReportQuery query) {
        validateRoundOwner(query.roundId(), query.userId());
        validateWalletExistsForExchange(query.roundId(), query.exchangeId());
        AnalysisExchange exchange = getExchangeInfo(query.exchangeId());
        AnalysisRules rules = findRules(query.roundId());

        RegretReport report =
                regretReportQueryPort.getByRoundIdAndExchangeId(
                        query.roundId(), query.exchangeId());

        return toResult(report, exchange, rules);
    }

    private void validateRoundOwner(Long roundId, Long userId) {
        AnalysisRound round = getRound(roundId);
        if (!round.userId().equals(userId)) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
    }

    private void validateWalletExistsForExchange(Long roundId, Long exchangeId) {
        if (findWalletUseCase.findByRoundIdAndExchangeId(roundId, exchangeId).isEmpty()) {
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }
    }

    private AnalysisRound getRound(Long roundId) {
        RoundInfoResult result =
                findRoundInfoUseCase
                        .findById(roundId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
        return toAnalysisRound(result);
    }

    private AnalysisRound toAnalysisRound(RoundInfoResult result) {
        return new AnalysisRound(
                result.roundId(),
                result.userId(),
                result.initialSeed(),
                AnalysisRoundStatus.valueOf(result.status()),
                result.startedAt(),
                result.endedAt());
    }

    private AnalysisExchange getExchangeInfo(Long exchangeId) {
        ExchangeDetailResult result =
                findExchangeDetailUseCase
                        .findExchangeDetail(exchangeId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        return toAnalysisExchange(exchangeId, result);
    }

    private AnalysisExchange toAnalysisExchange(Long exchangeId, ExchangeDetailResult result) {
        String currency = result.domestic() ? "KRW" : "USDT";
        return new AnalysisExchange(exchangeId, result.name(), currency);
    }

    private AnalysisRules findRules(Long roundId) {
        return new AnalysisRules(
                findInvestmentRulesUseCase.findByRoundId(roundId).stream()
                        .map(this::toAnalysisRule)
                        .toList());
    }

    private AnalysisRule toAnalysisRule(InvestmentRuleResult result) {
        return new AnalysisRule(result.ruleId(), result.ruleType(), result.thresholdValue());
    }

    private RegretReportResult toResult(
            RegretReport report, AnalysisExchange exchange, AnalysisRules rules) {
        Map<Long, AnalysisRule> ruleMap = rules.toMap();
        Map<Long, String> coinSymbols =
                findCoinSymbolsUseCase.findSymbolsByIds(
                        report.getViolationDetails().extractCoinIds());

        return RegretReportResult.from(report, exchange, ruleMap, coinSymbols);
    }
}
