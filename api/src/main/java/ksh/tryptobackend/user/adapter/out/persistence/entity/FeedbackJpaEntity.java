package ksh.tryptobackend.user.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import ksh.tryptobackend.user.domain.model.Feedback;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackJpaEntity {

    private static final int CONTENT_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long authorId;

    @Column(name = "content", nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static FeedbackJpaEntity fromDomain(Feedback feedback) {
        FeedbackJpaEntity entity = new FeedbackJpaEntity();
        entity.id = feedback.getFeedbackId();
        entity.authorId = feedback.getAuthorId();
        entity.content = feedback.getContent().value();
        entity.createdAt = feedback.getCreatedAt();
        return entity;
    }

    public Feedback toDomain() {
        return Feedback.reconstitute(id, authorId, content, createdAt);
    }
}
