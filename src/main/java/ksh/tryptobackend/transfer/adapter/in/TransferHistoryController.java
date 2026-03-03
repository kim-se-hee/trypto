package ksh.tryptobackend.transfer.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.dto.response.CursorPageResponseDto;
import ksh.tryptobackend.transfer.adapter.in.dto.request.GetTransferHistoryRequest;
import ksh.tryptobackend.transfer.adapter.in.dto.response.TransferHistoryResponse;
import ksh.tryptobackend.transfer.application.port.in.GetTransferHistoryUseCase;
import ksh.tryptobackend.transfer.application.port.in.dto.result.TransferHistoryCursorResult;
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

    private final GetTransferHistoryUseCase getTransferHistoryUseCase;

    @GetMapping
    public ApiResponseDto<CursorPageResponseDto<TransferHistoryResponse>> getTransferHistory(
        @PathVariable Long walletId,
        @Valid @ModelAttribute GetTransferHistoryRequest request
    ) {
        TransferHistoryCursorResult result = getTransferHistoryUseCase.getTransferHistory(request.toQuery(walletId));
        CursorPageResponseDto<TransferHistoryResponse> response = CursorPageResponseDto.of(
            result.transfers().stream()
                .map(transfer -> TransferHistoryResponse.from(transfer, walletId))
                .toList(),
            result.nextCursor(),
            result.hasNext());
        return ApiResponseDto.success("송금 내역을 조회했습니다.", response);
    }
}
