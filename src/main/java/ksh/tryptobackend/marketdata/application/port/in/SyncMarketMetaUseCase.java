package ksh.tryptobackend.marketdata.application.port.in;

public interface SyncMarketMetaUseCase {

    /**
     * @return true: 동기화 성공, false: Redis에 market-meta 데이터 없음
     */
    boolean sync();
}
