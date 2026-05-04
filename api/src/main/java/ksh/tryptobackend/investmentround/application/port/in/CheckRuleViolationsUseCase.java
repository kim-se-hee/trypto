package ksh.tryptobackend.investmentround.application.port.in;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RuleViolationResult;

public interface CheckRuleViolationsUseCase {

    List<RuleViolationResult> checkViolations(CheckRuleViolationsQuery query);
}
