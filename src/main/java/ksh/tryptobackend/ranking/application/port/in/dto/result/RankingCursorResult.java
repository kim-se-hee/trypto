package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.util.List;

public record RankingCursorResult(
    List<RankingItemResult> content,
    Integer nextCursor,
    boolean hasNext
) {
}
