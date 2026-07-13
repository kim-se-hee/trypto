package ksh.tryptobackend.user.adapter.out.persistence;

import ksh.tryptobackend.user.adapter.out.persistence.entity.FeedbackJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.FeedbackJpaRepository;
import ksh.tryptobackend.user.application.port.out.FeedbackCommandPort;
import ksh.tryptobackend.user.domain.model.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaFeedbackCommandAdapter implements FeedbackCommandPort {

    private final FeedbackJpaRepository feedbackJpaRepository;

    @Override
    public Feedback save(Feedback feedback) {
        return feedbackJpaRepository
                .save(FeedbackJpaEntity.fromDomain(feedback))
                .toDomain();
    }
}
