package ksh.tryptobackend.user.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.user.application.port.in.SendFeedbackUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.SendFeedbackCommand;
import ksh.tryptobackend.user.application.port.out.FeedbackCommandPort;
import ksh.tryptobackend.user.domain.model.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SendFeedbackService implements SendFeedbackUseCase {

    private final FeedbackCommandPort feedbackCommandPort;
    private final Clock clock;

    @Override
    @Transactional
    public Feedback sendFeedback(SendFeedbackCommand command) {
        Feedback feedback = Feedback.write(command.authorId(), command.content(), LocalDateTime.now(clock));
        return feedbackCommandPort.save(feedback);
    }
}
