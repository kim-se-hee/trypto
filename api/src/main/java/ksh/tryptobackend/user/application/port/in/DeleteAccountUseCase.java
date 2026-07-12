package ksh.tryptobackend.user.application.port.in;

import ksh.tryptobackend.user.application.port.in.dto.command.DeleteAccountCommand;

public interface DeleteAccountUseCase {

    void deleteAccount(DeleteAccountCommand command);
}
