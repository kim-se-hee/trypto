package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.ranking.adapter.out.entity.RankingJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.RankingUserJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingUserJpaRepository;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RankingMyStepDefinition {

    private final CommonApiClient apiClient;
    private final RankingJpaRepository rankingJpaRepository;
    private final RankingUserJpaRepository rankingUserJpaRepository;

    public RankingMyStepDefinition(CommonApiClient apiClient,
                                   RankingJpaRepository rankingJpaRepository,
                                   RankingUserJpaRepository rankingUserJpaRepository) {
        this.apiClient = apiClient;
        this.rankingJpaRepository = rankingJpaRepository;
        this.rankingUserJpaRepository = rankingUserJpaRepository;
    }

    @Before
    public void setUp() {
        rankingJpaRepository.deleteAllInBatch();
        rankingUserJpaRepository.deleteAllInBatch();
    }

    @Given("내 랭킹 테스트 데이터가 준비되어 있다")
    public void 내_랭킹_테스트_데이터가_준비되어_있다() {
        RankingUserJpaEntity user = createRankingUser(1L, "테스터", true);
        rankingUserJpaRepository.save(user);

        RankingJpaEntity ranking = createRanking(
            1L, 1L, RankingPeriod.DAILY, 1,
            new BigDecimal("12.5000"), 5,
            LocalDate.of(2026, 3, 1)
        );
        rankingJpaRepository.save(ranking);
    }

    @When("유저 {long}이 기간 {string}로 내 랭킹을 조회한다")
    public void 유저_이_기간_로_내_랭킹을_조회한다(long userId, String period) {
        apiClient.get("/api/rankings/me?userId=" + userId + "&period=" + period);
    }

    @Then("내 순위는 {int}이다")
    public void 내_순위는_이다(int rank) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.rank").isEqualTo(rank);
    }

    @Then("응답 data는 null이다")
    public void 응답_data는_null이다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data").isEqualTo(null);
    }

    private RankingUserJpaEntity createRankingUser(Long id, String nickname, boolean portfolioPublic) {
        RankingUserJpaEntity entity = instantiate(RankingUserJpaEntity.class);
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "nickname", nickname);
        ReflectionTestUtils.setField(entity, "portfolioPublic", portfolioPublic);
        return entity;
    }

    private RankingJpaEntity createRanking(Long userId, Long roundId, RankingPeriod period,
                                           int rank, BigDecimal profitRate, int tradeCount,
                                           LocalDate referenceDate) {
        RankingJpaEntity entity = instantiate(RankingJpaEntity.class);
        ReflectionTestUtils.setField(entity, "userId", userId);
        ReflectionTestUtils.setField(entity, "roundId", roundId);
        ReflectionTestUtils.setField(entity, "period", period);
        ReflectionTestUtils.setField(entity, "rank", rank);
        ReflectionTestUtils.setField(entity, "profitRate", profitRate);
        ReflectionTestUtils.setField(entity, "tradeCount", tradeCount);
        ReflectionTestUtils.setField(entity, "referenceDate", referenceDate);
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());
        return entity;
    }

    private <T> T instantiate(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
