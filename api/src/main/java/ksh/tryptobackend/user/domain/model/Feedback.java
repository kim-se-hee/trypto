package ksh.tryptobackend.user.domain.model;

import java.time.LocalDateTime;
import ksh.tryptobackend.user.domain.vo.FeedbackContent;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Feedback {

    private final Long feedbackId;
    private final Long authorId;
    private final FeedbackContent content;
    private final LocalDateTime createdAt;

    public static Feedback write(Long authorId, String content, LocalDateTime now) {
        return Feedback.builder()
                .feedbackId(null)
                .authorId(authorId)
                .content(FeedbackContent.of(content))
                .createdAt(now)
                .build();
    }

    public static Feedback reconstitute(Long feedbackId, Long authorId, String content, LocalDateTime createdAt) {
        return Feedback.builder()
                .feedbackId(feedbackId)
                .authorId(authorId)
                .content(FeedbackContent.of(content))
                .createdAt(createdAt)
                .build();
    }
}
