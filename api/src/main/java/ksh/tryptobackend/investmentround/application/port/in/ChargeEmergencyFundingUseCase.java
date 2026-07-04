package ksh.tryptobackend.investmentround.application.port.in;

import ksh.tryptobackend.investmentround.application.port.in.dto.command.ChargeEmergencyFundingCommand;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;

public interface ChargeEmergencyFundingUseCase {

    InvestmentRound charge(ChargeEmergencyFundingCommand command);
}
