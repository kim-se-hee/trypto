package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.in.GetRankingsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingsQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingCursorResult;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingItemResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingWithUserProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRankingsService implements GetRankingsUseCase {

    private final RankingQueryPort rankingQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RankingCursorResult getRankings(GetRankingsQuery query) {
        LocalDate referenceDate = resolveReferenceDate(query);
        List<RankingWithUserProjection> rankings = fetchRankingsWithOverflow(query, referenceDate);
        boolean hasNext = rankings.size() > query.size();
        List<RankingWithUserProjection> trimmed = hasNext ? rankings.subList(0, query.size()) : rankings;

        return buildCursorResult(trimmed, hasNext);
    }

    private LocalDate resolveReferenceDate(GetRankingsQuery query) {
        if (query.referenceDate() != null) {
            return query.referenceDate();
        }
        return rankingQueryPort.findLatestReferenceDate(query.period())
            .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
    }

    private List<RankingWithUserProjection> fetchRankingsWithOverflow(GetRankingsQuery query, LocalDate referenceDate) {
        return rankingQueryPort.findRankings(
            query.period(), referenceDate, query.cursorRank(), query.size() + 1);
    }

    private RankingCursorResult buildCursorResult(List<RankingWithUserProjection> rankings, boolean hasNext) {
        List<RankingItemResult> content = rankings.stream()
            .map(RankingItemResult::from)
            .toList();
        Integer nextCursor = hasNext ? rankings.getLast().rank() : null;

        return new RankingCursorResult(content, nextCursor, hasNext);
    }
}
