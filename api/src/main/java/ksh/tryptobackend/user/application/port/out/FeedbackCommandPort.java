package ksh.tryptobackend.user.application.port.out;

import ksh.tryptobackend.user.domain.model.Feedback;

public interface FeedbackCommandPort {

    Feedback save(Feedback feedback);
}
