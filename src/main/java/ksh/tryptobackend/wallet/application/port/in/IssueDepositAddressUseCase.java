package ksh.tryptobackend.wallet.application.port.in;

import ksh.tryptobackend.wallet.application.port.in.dto.command.IssueDepositAddressCommand;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;

public interface IssueDepositAddressUseCase {

    DepositAddress issueDepositAddress(IssueDepositAddressCommand command);
}
