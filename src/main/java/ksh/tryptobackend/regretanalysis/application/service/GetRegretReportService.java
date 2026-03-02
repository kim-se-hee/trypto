package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GetRegretReportResult;
import ksh.tryptobackend.regretanalysis.application.port.out.CoinSymbolPort;
import ksh.tryptobackend.regretanalysis.application.port.out.ExchangeMetadataPort;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundPort;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRulePort;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportPersistencePort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.ExchangeMetadata;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RoundInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RuleInfo;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRegretReportService implements GetRegretReportUseCase {

    private final InvestmentRoundPort investmentRoundPort;
    private final RegretReportPersistencePort regretReportPersistencePort;
    private final InvestmentRulePort investmentRulePort;
    private final ExchangeMetadataPort exchangeMetadataPort;
    private final CoinSymbolPort coinSymbolPort;

    @Override
    public GetRegretReportResult getRegretReport(GetRegretReportQuery query) {
        validateRoundOwner(query.roundId(), query.userId());
        validateWalletExistsForExchange(query.roundId(), query.exchangeId());
        ExchangeMetadata exchange = exchangeMetadataPort.getExchangeMetadata(query.exchangeId());
        List<RuleInfo> rules = investmentRulePort.findByRoundId(query.roundId());

        RegretReport report = regretReportPersistencePort.getByRoundIdAndExchangeId(
            query.roundId(), query.exchangeId());

        return toResult(report, exchange, rules);
    }

    private void validateRoundOwner(Long roundId, Long userId) {
        RoundInfoResult round = investmentRoundPort.getRound(roundId);
        if (!round.userId().equals(userId)) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
    }

    private void validateWalletExistsForExchange(Long roundId, Long exchangeId) {
        if (!exchangeMetadataPort.existsWalletForExchange(roundId, exchangeId)) {
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }
    }

    private GetRegretReportResult toResult(RegretReport report, ExchangeMetadata exchange,
                                            List<RuleInfo> rules) {
        Map<Long, RuleInfo> ruleMap = rules.stream()
            .collect(Collectors.toMap(RuleInfo::ruleId, r -> r));
        Map<Long, String> coinSymbols = coinSymbolPort.findSymbolsByIds(
            report.getViolationDetails().extractCoinIds());

        return GetRegretReportResult.from(report, exchange, ruleMap, coinSymbols);
    }
}
