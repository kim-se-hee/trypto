package ksh.tryptobackend.trading.domain.service;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.TradingContext;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Balances {

    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;

    public void validateFor(Order order, TradingContext ctx) {
        BigDecimal available =
                getAvailableBalanceUseCase.getAvailableBalance(
                        order.getWalletId(), ctx.balanceCoinId());
        order.validateSufficientBalance(available);

        for (BalanceChange change :
                ctx.mode().planBalanceChanges(order, ctx.venue(), ctx.coinId())) {
            applyChange(order.getWalletId(), change);
        }
    }

    private void applyChange(Long walletId, BalanceChange change) {
        switch (change) {
            case BalanceChange.Deduct d ->
                    manageWalletBalanceUseCase.deductBalance(walletId, d.coinId(), d.amount());
            case BalanceChange.Add a ->
                    manageWalletBalanceUseCase.addBalance(walletId, a.coinId(), a.amount());
            case BalanceChange.Lock l ->
                    manageWalletBalanceUseCase.lockBalance(walletId, l.coinId(), l.amount());
        }
    }
}
