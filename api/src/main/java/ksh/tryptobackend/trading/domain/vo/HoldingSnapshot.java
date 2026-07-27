package ksh.tryptobackend.trading.domain.vo;

/**
 * 주문 시점의 보유 상태.
 *
 * <p>위반 판정은 커밋 이후에 실행되는데, 시장가 주문은 그 전에 체결 정산이 끝나 보유 정보가 이미 바뀐다. 판정이 이번 주문을 두 번 세지 않도록 주문 시점 값을 담아둔다.
 */
public record HoldingSnapshot(boolean atLoss, int averagingDownCount) {

    public static HoldingSnapshot empty() {
        return new HoldingSnapshot(false, 0);
    }
}
