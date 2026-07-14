package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.util.List;

public record RankingCursorResult(List<RankingItemResult> content, Integer nextCursor, boolean hasNext) {

    public static RankingCursorResult empty() {
        return new RankingCursorResult(List.of(), null, false);
    }
}
