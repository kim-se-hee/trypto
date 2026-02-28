package ksh.tryptobackend.ranking.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.dto.response.CursorPageResponseDto;
import ksh.tryptobackend.ranking.adapter.in.dto.request.GetMyRankingRequest;
import ksh.tryptobackend.ranking.adapter.in.dto.request.GetRankingsRequest;
import ksh.tryptobackend.ranking.adapter.in.dto.response.MyRankingResponse;
import ksh.tryptobackend.ranking.adapter.in.dto.response.RankingItemResponse;
import ksh.tryptobackend.ranking.application.port.in.GetMyRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.GetRankingsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.result.MyRankingResult;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingCursorResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final GetRankingsUseCase getRankingsUseCase;
    private final GetMyRankingUseCase getMyRankingUseCase;

    @GetMapping
    public ApiResponseDto<CursorPageResponseDto<RankingItemResponse>> getRankings(
        @Valid @ModelAttribute GetRankingsRequest request
    ) {
        RankingCursorResult result = getRankingsUseCase.getRankings(request.toQuery());
        CursorPageResponseDto<RankingItemResponse> response = CursorPageResponseDto.of(
            result.content().stream().map(RankingItemResponse::from).toList(),
            result.nextCursor() != null ? result.nextCursor().longValue() : null,
            result.hasNext());
        return ApiResponseDto.success("랭킹을 조회했습니다.", response);
    }

    @GetMapping("/me")
    public ApiResponseDto<MyRankingResponse> getMyRanking(@Valid @ModelAttribute GetMyRankingRequest request) {
        MyRankingResult result = getMyRankingUseCase.getMyRanking(request.toQuery());
        MyRankingResponse response = result != null ? MyRankingResponse.from(result) : null;
        return ApiResponseDto.success("내 랭킹을 조회했습니다.", response);
    }
}
