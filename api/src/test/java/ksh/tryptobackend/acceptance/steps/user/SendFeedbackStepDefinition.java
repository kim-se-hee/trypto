package ksh.tryptobackend.acceptance.steps.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.user.adapter.out.persistence.entity.FeedbackJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.FeedbackJpaRepository;

public class SendFeedbackStepDefinition {

    private static final String FEEDBACK_PATH = "/api/feedbacks";

    private final CommonApiClient apiClient;
    private final FeedbackJpaRepository feedbackJpaRepository;

    public SendFeedbackStepDefinition(CommonApiClient apiClient, FeedbackJpaRepository feedbackJpaRepository) {
        this.apiClient = apiClient;
        this.feedbackJpaRepository = feedbackJpaRepository;
    }

    @When("{string}라는 내용으로 피드백을 보낸다")
    public void 라는_내용으로_피드백을_보낸다(String content) {
        apiClient.post(FEEDBACK_PATH, Map.of("content", content));
    }

    @When("{string}이라는 내용으로 피드백을 보낸다")
    public void 이라는_내용으로_피드백을_보낸다(String content) {
        apiClient.post(FEEDBACK_PATH, Map.of("content", content));
    }

    @When("세션 쿠키 없이 {string}라는 내용으로 피드백을 보낸다")
    public void 세션_쿠키_없이_라는_내용으로_피드백을_보낸다(String content) {
        apiClient.post(FEEDBACK_PATH, Map.of("content", content));
    }

    @Then("저장된 피드백의 작성자는 로그인한 사용자다")
    public void 저장된_피드백의_작성자는_로그인한_사용자다() {
        List<FeedbackJpaEntity> feedbacks = feedbackJpaRepository.findAll();

        assertThat(feedbacks).hasSize(1);
        assertThat(feedbacks.getFirst().getAuthorId()).isEqualTo(apiClient.getLoggedInUserId());
    }
}
