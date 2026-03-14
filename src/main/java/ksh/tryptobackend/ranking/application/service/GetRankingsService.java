package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.in.GetRankingsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingsQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingCursorResult;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingItemResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;
import ksh.tryptobackend.user.application.port.in.FindUserPublicInfoUseCase;
import ksh.tryptobackend.user.application.port.in.dto.result.UserPublicInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRankingsService implements GetRankingsUseCase {

    private final RankingQueryPort rankingQueryPort;
    private final FindUserPublicInfoUseCase findUserPublicInfoUseCase;

    @Override
    @Transactional(readOnly = true)
    public RankingCursorResult getRankings(GetRankingsQuery query) {
        LocalDate referenceDate = resolveReferenceDate(query);
        List<RankingSummary> rankings = fetchRankingsWithOverflow(query, referenceDate);
        boolean hasNext = rankings.size() > query.size();
        List<RankingSummary> trimmed = hasNext ? rankings.subList(0, query.size()) : rankings;

        Map<Long, UserPublicInfoResult> userInfoMap = resolveUserInfo(trimmed);
        return buildCursorResult(trimmed, userInfoMap, hasNext);
    }

    private LocalDate resolveReferenceDate(GetRankingsQuery query) {
        if (query.referenceDate() != null) {
            return query.referenceDate();
        }
        return rankingQueryPort.findLatestReferenceDate(query.period())
            .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
    }

    private List<RankingSummary> fetchRankingsWithOverflow(GetRankingsQuery query, LocalDate referenceDate) {
        return rankingQueryPort.findRankings(
            query.period(), referenceDate, query.cursorRank(), query.size() + 1);
    }

    private Map<Long, UserPublicInfoResult> resolveUserInfo(List<RankingSummary> rankings) {
        Set<Long> userIds = rankings.stream()
            .map(RankingSummary::userId)
            .collect(Collectors.toSet());
        return findUserPublicInfoUseCase.findByUserIds(userIds).stream()
            .collect(Collectors.toMap(UserPublicInfoResult::userId, info -> info));
    }

    private RankingCursorResult buildCursorResult(List<RankingSummary> rankings,
                                                    Map<Long, UserPublicInfoResult> userInfoMap,
                                                    boolean hasNext) {
        List<RankingItemResult> content = rankings.stream()
            .map(r -> toRankingItemResult(r, userInfoMap))
            .toList();
        Integer nextCursor = hasNext ? rankings.getLast().rank() : null;

        return new RankingCursorResult(content, nextCursor, hasNext);
    }

    private RankingItemResult toRankingItemResult(RankingSummary ranking,
                                                    Map<Long, UserPublicInfoResult> userInfoMap) {
        UserPublicInfoResult userInfo = userInfoMap.get(ranking.userId());
        return new RankingItemResult(
            ranking.rank(),
            ranking.userId(),
            userInfo != null ? userInfo.nickname() : "",
            ranking.profitRate(),
            ranking.tradeCount(),
            userInfo != null && userInfo.portfolioPublic()
        );
    }
}
