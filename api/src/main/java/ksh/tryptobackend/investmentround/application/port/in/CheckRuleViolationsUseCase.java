package ksh.tryptobackend.investmentround.application.port.in;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RuleViolationCheckResult;

public interface CheckRuleViolationsUseCase {

    List<RuleViolationCheckResult> checkViolations(CheckRuleViolationsQuery query);
}
