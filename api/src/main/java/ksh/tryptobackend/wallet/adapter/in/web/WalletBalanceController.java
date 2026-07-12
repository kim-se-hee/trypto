package ksh.tryptobackend.wallet.adapter.in.web;

import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.web.auth.LoginUser;
import ksh.tryptobackend.wallet.adapter.in.dto.response.WalletBalancesResponse;
import ksh.tryptobackend.wallet.application.port.in.GetWalletBalancesUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.query.GetWalletBalancesQuery;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletBalancesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets/{walletId}/balances")
@RequiredArgsConstructor
public class WalletBalanceController {

    private final GetWalletBalancesUseCase getWalletBalancesUseCase;

    @GetMapping
    public ApiResponseDto<WalletBalancesResponse> getWalletBalances(
            @LoginUser Long userId, @PathVariable Long walletId) {
        GetWalletBalancesQuery query = new GetWalletBalancesQuery(userId, walletId);
        WalletBalancesResult result = getWalletBalancesUseCase.getWalletBalances(query);
        return ApiResponseDto.success("잔고를 조회했습니다.", WalletBalancesResponse.from(result));
    }
}
