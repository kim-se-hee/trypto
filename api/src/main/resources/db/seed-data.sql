-- 인수 테스트용 마스터 데이터 시드.
-- 컨텍스트 부팅 시 1회만 적재되고, DatabaseCleanupHook 의 TRUNCATE 대상에서 영구 제외된다.
-- 시나리오는 이 데이터를 수정하지 않는다는 약속하에 동작한다.

INSERT IGNORE INTO coin (coin_id, symbol, name) VALUES
    (1, 'KRW', '원화'),
    (2, 'BTC', '비트코인'),
    (3, 'ETH', '이더리움');

INSERT IGNORE INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id, fee_rate) VALUES
    (1, 'UPBIT', 'DOMESTIC', 1, 0.000500),
    (2, 'BITHUMB', 'DOMESTIC', 1, 0.002500),
    (3, 'BINANCE', 'OVERSEAS', 1, 0.001000);

INSERT IGNORE INTO exchange_coin (exchange_coin_id, exchange_id, coin_id, display_name) VALUES
    (10, 1, 2, '비트코인'),
    (11, 1, 3, '이더리움');
