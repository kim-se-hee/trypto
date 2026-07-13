package ksh.tryptobackend.user.application.port.in;

import ksh.tryptobackend.user.application.port.in.dto.command.SendFeedbackCommand;
import ksh.tryptobackend.user.domain.model.Feedback;

public interface SendFeedbackUseCase {

    Feedback sendFeedback(SendFeedbackCommand command);
}
