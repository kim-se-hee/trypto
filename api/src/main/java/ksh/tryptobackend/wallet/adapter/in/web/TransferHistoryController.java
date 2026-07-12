package ksh.tryptobackend.wallet.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.dto.response.CursorPageResponseDto;
import ksh.tryptobackend.common.web.auth.LoginUser;
import ksh.tryptobackend.wallet.adapter.in.dto.request.FindTransferHistoryRequest;
import ksh.tryptobackend.wallet.adapter.in.dto.response.TransferHistoryResponse;
import ksh.tryptobackend.wallet.application.port.in.FindTransferHistoryUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferHistoryCursorResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets/{walletId}/transfers")
@RequiredArgsConstructor
public class TransferHistoryController {

    private final FindTransferHistoryUseCase findTransferHistoryUseCase;

    @GetMapping
    public ApiResponseDto<CursorPageResponseDto<TransferHistoryResponse>> findTransferHistory(
            @PathVariable Long walletId,
            @LoginUser Long userId,
            @Valid @ModelAttribute FindTransferHistoryRequest request) {
        TransferHistoryCursorResult result =
                findTransferHistoryUseCase.findTransferHistory(request.toQuery(walletId, userId));
        CursorPageResponseDto<TransferHistoryResponse> response = CursorPageResponseDto.of(
                result.content().stream().map(TransferHistoryResponse::from).toList(),
                result.nextCursor(),
                result.hasNext());
        return ApiResponseDto.success("송금 내역을 조회했습니다.", response);
    }
}
