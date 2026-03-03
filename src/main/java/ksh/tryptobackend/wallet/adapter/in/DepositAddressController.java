package ksh.tryptobackend.wallet.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.wallet.adapter.in.dto.request.GetDepositAddressRequest;
import ksh.tryptobackend.wallet.adapter.in.dto.response.DepositAddressResponse;
import ksh.tryptobackend.wallet.application.port.in.IssueDepositAddressUseCase;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets/{walletId}/deposit-address")
@RequiredArgsConstructor
public class DepositAddressController {

    private final IssueDepositAddressUseCase issueDepositAddressUseCase;

    @GetMapping
    public ApiResponseDto<DepositAddressResponse> getDepositAddress(
            @PathVariable Long walletId,
            @Valid @ModelAttribute GetDepositAddressRequest request) {
        DepositAddress depositAddress = issueDepositAddressUseCase.issueDepositAddress(request.toCommand(walletId));
        return ApiResponseDto.success("조회 성공", DepositAddressResponse.from(depositAddress));
    }
}
