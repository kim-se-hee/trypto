package ksh.tryptobackend.ranking.application.service;

import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.in.GetRankingsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingsQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingCursorResult;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingItemResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.UserQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RankingSummaries;
import ksh.tryptobackend.ranking.domain.vo.UserProfiles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRankingsService implements GetRankingsUseCase {

    private final RankingQueryPort rankingQueryPort;
    private final UserQueryPort userQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RankingCursorResult getRankings(GetRankingsQuery query) {
        LocalDate referenceDate = resolveReferenceDate(query);
        RankingSummaries page = RankingSummaries.fromOverflow(
                rankingQueryPort.findRankings(query.period(), referenceDate, query.cursorRank(), query.size() + 1),
                query.size());
        UserProfiles userProfiles = userQueryPort.findByUserIds(page.userIds());
        List<RankingItemResult> content = page.toList().stream()
                .map(summary -> RankingItemResult.of(summary, userProfiles))
                .toList();
        return new RankingCursorResult(content, page.nextCursorRank(), page.hasNext());
    }

    private LocalDate resolveReferenceDate(GetRankingsQuery query) {
        if (query.referenceDate() != null) {
            return query.referenceDate();
        }
        return rankingQueryPort
                .findLatestReferenceDate(query.period())
                .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
    }
}
