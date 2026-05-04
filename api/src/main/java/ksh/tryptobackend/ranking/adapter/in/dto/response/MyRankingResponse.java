package ksh.tryptobackend.ranking.adapter.in.dto.response;

import java.math.BigDecimal;
import ksh.tryptobackend.ranking.application.port.in.dto.result.MyRankingResult;

public record MyRankingResponse(int rank, String nickname, BigDecimal profitRate, int tradeCount) {

    public static MyRankingResponse from(MyRankingResult result) {
        return new MyRankingResponse(
                result.rank(), result.nickname(), result.profitRate().value(), result.tradeCount());
    }
}
