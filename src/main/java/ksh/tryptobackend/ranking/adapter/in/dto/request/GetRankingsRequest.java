package ksh.tryptobackend.ranking.adapter.in.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingsQuery;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record GetRankingsRequest(
    @NotNull RankingPeriod period,
    @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate referenceDate,
    @Min(1) Integer cursorRank,
    @Min(1) @Max(50) Integer size
) {

    public GetRankingsRequest {
        if (size == null) size = 20;
    }

    public GetRankingsQuery toQuery() {
        return new GetRankingsQuery(period, referenceDate, cursorRank, size);
    }
}
