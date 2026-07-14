package ksh.tryptobackend.ranking.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
        return resolveReferenceDate(query)
                .map(referenceDate -> findRankings(query, referenceDate))
                .orElseGet(RankingCursorResult::empty);
    }

    // 집계된 랭킹이 하나도 없으면 기준 날짜를 정할 수 없다. 배치가 아직 돌지 않은 정상 상태이므로 빈 결과로 응답한다.
    private Optional<LocalDate> resolveReferenceDate(GetRankingsQuery query) {
        if (query.referenceDate() != null) {
            return Optional.of(query.referenceDate());
        }
        return rankingQueryPort.findLatestReferenceDate(query.period());
    }

    private RankingCursorResult findRankings(GetRankingsQuery query, LocalDate referenceDate) {
        RankingSummaries page = RankingSummaries.fromOverflow(
                rankingQueryPort.findRankings(query.period(), referenceDate, query.cursorRank(), query.size() + 1),
                query.size());
        UserProfiles userProfiles = userQueryPort.findByUserIds(page.userIds());
        List<RankingItemResult> content = page.toList().stream()
                .map(summary -> RankingItemResult.of(summary, userProfiles))
                .toList();
        return new RankingCursorResult(content, page.nextCursorRank(), page.hasNext());
    }
}
