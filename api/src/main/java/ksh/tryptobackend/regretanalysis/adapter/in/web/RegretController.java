package ksh.tryptobackend.regretanalysis.adapter.in.web;

import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.web.auth.LoginUser;
import ksh.tryptobackend.regretanalysis.adapter.in.dto.response.RegretChartResponse;
import ksh.tryptobackend.regretanalysis.adapter.in.dto.response.RegretReportResponse;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretChartUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretChartQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds/{roundId}/regret")
@RequiredArgsConstructor
public class RegretController {

    private final GetRegretReportUseCase getRegretReportUseCase;
    private final GetRegretChartUseCase getRegretChartUseCase;

    @GetMapping
    public ApiResponseDto<RegretReportResponse> getRegretReport(@PathVariable Long roundId, @LoginUser Long userId) {
        RegretReportResult result = getRegretReportUseCase.getRegretReport(new GetRegretReportQuery(userId, roundId));
        return ApiResponseDto.success("투자 복기 리포트를 조회했습니다.", RegretReportResponse.from(result));
    }

    @GetMapping("/chart")
    public ApiResponseDto<RegretChartResponse> getRegretChart(@PathVariable Long roundId, @LoginUser Long userId) {
        RegretChartResult result = getRegretChartUseCase.getRegretChart(new GetRegretChartQuery(roundId, userId));
        return ApiResponseDto.success("복기 그래프 데이터를 조회했습니다.", RegretChartResponse.from(result));
    }
}
